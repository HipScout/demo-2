package com.example.gitlabscanner;

import com.example.gitlabscanner.client.GitLabApiClient;
import com.example.gitlabscanner.dto.GitLabProjectDTO;
import com.example.gitlabscanner.dto.GitLabTreeItemDTO;
import com.example.gitlabscanner.model.Project;
import com.example.gitlabscanner.model.ScanResult;
import com.example.gitlabscanner.scanner.ExposedSecretsScanner;
import com.example.gitlabscanner.scanner.MissingMetadataScanner;
import com.example.gitlabscanner.scanner.SensitiveFilesScanner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScannerService {
    private final GitLabApiClient gitLabApiClient;
    private final SensitiveFilesScanner sensitiveFilesScanner;
    private final ExposedSecretsScanner exposedSecretsScanner;
    private final MissingMetadataScanner missingMetadataScanner;

    public ScannerService(GitLabApiClient gitLabApiClient,
                         SensitiveFilesScanner sensitiveFilesScanner,
                         ExposedSecretsScanner exposedSecretsScanner,
                         MissingMetadataScanner missingMetadataScanner) {
        this.gitLabApiClient = gitLabApiClient;
        this.sensitiveFilesScanner = sensitiveFilesScanner;
        this.exposedSecretsScanner = exposedSecretsScanner;
        this.missingMetadataScanner = missingMetadataScanner;
    }

    public ScanResult scanUserProjects(String username, String gitlabToken) {
        if (gitlabToken != null && !gitlabToken.isBlank()) {
            gitLabApiClient.setGitlabToken(gitlabToken);
        }
        boolean hasToken = isTokenConfigured();
        List<GitLabProjectDTO> projects = gitLabApiClient.getPublicProjectsByUsername(username);
        return performScan(username, null, projects, hasToken);
    }

    public ScanResult scanGroupProjects(String groupName, String gitlabToken) {
        if (gitlabToken != null && !gitlabToken.isBlank()) {
            gitLabApiClient.setGitlabToken(gitlabToken);
        }
        boolean hasToken = isTokenConfigured();
        List<GitLabProjectDTO> projects = gitLabApiClient.getGroupPublicProjects(groupName);
        return performScan(null, groupName, projects, hasToken);
    }

    private boolean isTokenConfigured() {
        return gitLabApiClient.getGitlabToken() != null && !gitLabApiClient.getGitlabToken().isBlank();
    }

    private ScanResult performScan(String username, String groupName, List<GitLabProjectDTO> projects, boolean hasToken) {
        ScanResult result = new ScanResult();
        result.setUsername(username);
        result.setGroupName(groupName);
        result.setScanTime(LocalDateTime.now());

        if (projects == null) {
            projects = List.of();
        }

        List<Project> scannedProjects = new ArrayList<>();
        int projectsWithRisks = 0;

        for (GitLabProjectDTO dto : projects) {
            Boolean isPublic = dto.getIsPublic();
            boolean isPublicRepo = (isPublic != null && isPublic) || 
                                   "public".equalsIgnoreCase(dto.getVisibility());

            // If no token is provided, strictly filter for public repositories
            if (!hasToken && !isPublicRepo) {
                continue;
            }

            Project project = new Project();
            project.setId(dto.getId());
            project.setName(dto.getName());
            project.setWebUrl(dto.getWebUrl());
            project.setDescription(dto.getDescription());
            project.setIsPublic(isPublicRepo);

            // Fetch repository tree once (recursive)
            List<GitLabTreeItemDTO> treeItems = gitLabApiClient.getRepositoryTree(dto.getId(), null);

            // 1. Scan for sensitive files
            project.getRisks().addAll(sensitiveFilesScanner.scan(treeItems));

            // 2. Scan for missing metadata
            project.getRisks().addAll(missingMetadataScanner.scan(treeItems));

            // 3. Scan text files for exposed secrets (limit to reasonable batch to avoid rate-limiting)
            int secretsFilesScanned = 0;
            for (GitLabTreeItemDTO item : treeItems) {
                if ("blob".equalsIgnoreCase(item.getType()) && isTextFile(item.getName())) {
                    project.getRisks().addAll(exposedSecretsScanner.scan(gitLabApiClient, dto.getId(), item.getPath()));
                    secretsFilesScanned++;
                    if (secretsFilesScanned >= 25) {
                        break;
                    }
                }
            }

            if (!project.getRisks().isEmpty()) {
                projectsWithRisks++;
            }

            scannedProjects.add(project);
        }

        result.setTotalProjects(scannedProjects.size());
        result.setProjectsScanned(scannedProjects);
        result.setProjectsWithRisks(projectsWithRisks);
        result.setHighestSeverity(result.calculateHighestSeverity());

        return result;
    }

    private boolean isTextFile(String filename) {
        String[] textExtensions = {".java", ".js", ".py", ".rb", ".go", ".rs", ".yml", ".yaml",
                                   ".json", ".xml", ".config", ".conf", ".sh", ".bash", ".txt",
                                   ".md", ".env", ".properties", ".gradle", ".pom", ".tf"};
        return java.util.Arrays.stream(textExtensions)
            .anyMatch(filename.toLowerCase()::endsWith);
    }
}
