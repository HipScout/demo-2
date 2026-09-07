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
        if (gitlabToken != null && !gitlabToken.isEmpty()) {
            gitLabApiClient.setGitlabToken(gitlabToken);
        }

        List<GitLabProjectDTO> projects = gitLabApiClient.getPublicProjectsByUsername(username);
        return performScan(username, null, projects);
    }

    public ScanResult scanGroupProjects(String groupName, String gitlabToken) {
        if (gitlabToken != null && !gitlabToken.isEmpty()) {
            gitLabApiClient.setGitlabToken(gitlabToken);
        }

        List<GitLabProjectDTO> projects = gitLabApiClient.getGroupPublicProjects(groupName);
        return performScan(null, groupName, projects);
    }

    private ScanResult performScan(String username, String groupName, List<GitLabProjectDTO> projects) {
        ScanResult result = new ScanResult();
        result.setUsername(username);
        result.setGroupName(groupName);
        result.setScanTime(LocalDateTime.now());
        result.setTotalProjects(projects.size());

        List<Project> scannedProjects = new ArrayList<>();
        int projectsWithRisks = 0;

        for (GitLabProjectDTO dto : projects) {
            // Check if project is public (consider null as false)
            Boolean isPublic = dto.getIsPublic();
            if (isPublic == null || !isPublic) {
                // Also check visibility field
                if (dto.getVisibility() == null || !dto.getVisibility().equalsIgnoreCase("public")) {
                    continue;
                }
            }

            Project project = new Project();
            project.setId(dto.getId());
            project.setName(dto.getName());
            project.setWebUrl(dto.getWebUrl());
            project.setDescription(dto.getDescription());
            project.setIsPublic(isPublic != null ? isPublic : true);

            // Scan for sensitive files
            project.getRisks().addAll(sensitiveFilesScanner.scan(gitLabApiClient, dto.getId()));

            // Scan for missing metadata
            project.getRisks().addAll(missingMetadataScanner.scan(gitLabApiClient, dto.getId()));

            // Scan files for exposed secrets
            List<GitLabTreeItemDTO> treeItems = gitLabApiClient.getRepositoryTree(dto.getId(), null);
            for (GitLabTreeItemDTO item : treeItems) {
                if (item.getType().equals("blob") && isTextFile(item.getName())) {
                    project.getRisks().addAll(exposedSecretsScanner.scan(gitLabApiClient, dto.getId(), item.getPath()));
                }
            }

            if (!project.getRisks().isEmpty()) {
                projectsWithRisks++;
            }

            scannedProjects.add(project);
        }

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
