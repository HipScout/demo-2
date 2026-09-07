package com.example.gitlabscanner.client;

import com.example.gitlabscanner.dto.GitLabFileContentDTO;
import com.example.gitlabscanner.dto.GitLabProjectDTO;
import com.example.gitlabscanner.dto.GitLabTreeItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    private RestClient.RequestHeadersUriSpec<?> requestWithAuth() {
        return (RestClient.RequestHeadersUriSpec<?>) restClient.get()
            .headers(headers -> {
                if (gitlabToken != null && !gitlabToken.isBlank()) {
                    headers.set("PRIVATE-TOKEN", gitlabToken.trim());
                }
            });
    }

    public List<GitLabProjectDTO> getPublicProjectsByUsername(String username) {
        try {
            Long userId = resolveUserId(username);
            String url;
            if (userId != null) {
                url = "/users/" + userId + "/projects?per_page=100";
            } else {
                url = "/users/" + URLEncoder.encode(username, StandardCharsets.UTF_8) + "/projects?per_page=100";
            }
            GitLabProjectDTO[] projects = requestWithAuth()
                .uri(url)
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

    private Long resolveUserId(String username) {
        if (username.matches("\\d+")) {
            return Long.parseLong(username);
        }
        try {
            String url = "/users?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object>[] users = requestWithAuth()
                .uri(url)
                .retrieve()
                .body(Map[].class);
            if (users != null && users.length > 0 && users[0].containsKey("id")) {
                Object idObj = users[0].get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not resolve user ID for username {}: {}", username, e.getMessage());
        }
        return null;
    }

    public List<GitLabProjectDTO> getGroupPublicProjects(String groupName) {
        try {
            String encodedGroup = URLEncoder.encode(groupName, StandardCharsets.UTF_8);
            String url = "/groups/" + encodedGroup + "/projects?per_page=100";
            GitLabProjectDTO[] projects = requestWithAuth()
                .uri(url)
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
        return getRepositoryTree(projectId, path, true);
    }

    public List<GitLabTreeItemDTO> getRepositoryTree(Long projectId, String path, boolean recursive) {
        try {
            StringBuilder url = new StringBuilder("/projects/" + projectId + "/repository/tree?per_page=100&recursive=" + recursive);
            if (path != null && !path.isBlank()) {
                url.append("&path=").append(URLEncoder.encode(path, StandardCharsets.UTF_8));
            }
            
            GitLabTreeItemDTO[] items = requestWithAuth()
                .uri(url.toString())
                .retrieve()
                .body(GitLabTreeItemDTO[].class);
            
            return Arrays.asList(items != null ? items : new GitLabTreeItemDTO[0]);
        } catch (Exception e) {
            logger.debug("Error fetching tree for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    public String getFileContent(Long projectId, String filePath) {
        try {
            String encodedPath = encodeFilePath(filePath);
            URI uri = URI.create(GITLAB_API_URL + "/projects/" + projectId + "/repository/files/" + encodedPath + "/raw?ref=HEAD");
            return requestWithAuth()
                .uri(uri)
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
            URI uri = URI.create(GITLAB_API_URL + "/projects/" + projectId + "/repository/files/" + encodedPath + "?ref=HEAD");
            requestWithAuth()
                .uri(uri)
                .retrieve()
                .body(GitLabFileContentDTO.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String encodeFilePath(String filePath) {
        return URLEncoder.encode(filePath, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public void setGitlabToken(String token) {
        this.gitlabToken = token;
    }

    public String getGitlabToken() {
        return gitlabToken;
    }
}
