package com.example.gitlabscanner.client;

import com.example.gitlabscanner.dto.GitLabProjectDTO;
import com.example.gitlabscanner.dto.GitLabTreeItemDTO;
import com.example.gitlabscanner.dto.GitLabFileContentDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Base64;

@Component
public class GitLabApiClient {
    private static final String GITLAB_API_URL = "https://gitlab.com/api/v4";
    private static final Logger logger = LoggerFactory.getLogger(GitLabApiClient.class);
    
    private final RestClient restClient;
    private String gitlabToken;

    public GitLabApiClient(RestClient.Builder restClientBuilder, 
                          @Value("${gitlab.token:}") String gitlabToken) {
        this.gitlabToken = gitlabToken;
        this.restClient = restClientBuilder
            .baseUrl(GITLAB_API_URL)
            .defaultHeader("Accept", "application/json")
            .build();
    }

    public List<GitLabProjectDTO> getPublicProjectsByUsername(String username) {
        try {
            String url = "/users/{username}/projects?per_page=100";
            GitLabProjectDTO[] projects = restClient.get()
                .uri(url, username)
                .retrieve()
                .body(GitLabProjectDTO[].class);
            
            List<GitLabProjectDTO> result = Arrays.asList(projects != null ? projects : new GitLabProjectDTO[0]);
            logger.info("Fetched {} projects for user: {}", result.size(), username);
            return result;
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("User not found: {}", username);
            return List.of();
        } catch (Exception e) {
            logger.error("Error fetching projects for user: {}", username, e);
            return List.of();
        }
    }

    public List<GitLabProjectDTO> getGroupPublicProjects(String groupName) {
        try {
            String url = "/groups/{groupName}/projects?per_page=100";
            GitLabProjectDTO[] projects = restClient.get()
                .uri(url, groupName)
                .retrieve()
                .body(GitLabProjectDTO[].class);
            
            List<GitLabProjectDTO> result = Arrays.asList(projects != null ? projects : new GitLabProjectDTO[0]);
            logger.info("Fetched {} projects for group: {}", result.size(), groupName);
            return result;
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Group not found: {}", groupName);
            return List.of();
        } catch (Exception e) {
            logger.error("Error fetching projects for group: {}", groupName, e);
            return List.of();
        }
    }

    public List<GitLabTreeItemDTO> getRepositoryTree(Long projectId, String path) {
        try {
            String url = "/projects/{projectId}/repository/tree?recursive=false&per_page=100";
            if (path != null && !path.isEmpty()) {
                url += "&path={path}";
            }
            
            GitLabTreeItemDTO[] items;
            if (path != null && !path.isEmpty()) {
                items = restClient.get()
                    .uri(url, projectId, path)
                    .retrieve()
                    .body(GitLabTreeItemDTO[].class);
            } else {
                items = restClient.get()
                    .uri(url, projectId)
                    .retrieve()
                    .body(GitLabTreeItemDTO[].class);
            }
            
            return Arrays.asList(items != null ? items : new GitLabTreeItemDTO[0]);
        } catch (Exception e) {
            logger.debug("Error fetching tree for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    public String getFileContent(Long projectId, String filePath) {
        try {
            String encodedPath = encodeFilePath(filePath);
            String url = "/projects/{projectId}/repository/files/{filePath}/raw?ref=HEAD";
            return restClient.get()
                .uri(url, projectId, encodedPath)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            logger.debug("Error fetching file content for {}: {}", filePath, e.getMessage());
            return null;
        }
    }

    public boolean fileExists(Long projectId, String filePath) {
        try {
            String encodedPath = encodeFilePath(filePath);
            String url = "/projects/{projectId}/repository/files/{filePath}?ref=HEAD";
            restClient.get()
                .uri(url, projectId, encodedPath)
                .retrieve()
                .body(GitLabFileContentDTO.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String encodeFilePath(String filePath) {
        return Base64.getEncoder().encodeToString(filePath.getBytes()).replaceAll("=", "%3D");
    }

    public void setGitlabToken(String token) {
        this.gitlabToken = token;
    }
}
