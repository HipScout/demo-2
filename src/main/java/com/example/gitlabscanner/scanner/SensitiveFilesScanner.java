package com.example.gitlabscanner.scanner;

import com.example.gitlabscanner.client.GitLabApiClient;
import com.example.gitlabscanner.dto.GitLabTreeItemDTO;
import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.RiskCategory;
import com.example.gitlabscanner.model.SeverityLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SensitiveFilesScanner {

    public List<Risk> scan(GitLabApiClient client, Long projectId) {
        List<GitLabTreeItemDTO> treeItems = client.getRepositoryTree(projectId, null);
        return scan(treeItems);
    }

    public List<Risk> scan(List<GitLabTreeItemDTO> treeItems) {
        List<Risk> risks = new ArrayList<>();
        if (treeItems == null) {
            return risks;
        }

        for (GitLabTreeItemDTO item : treeItems) {
            if ("blob".equalsIgnoreCase(item.getType())) {
                String filename = item.getName() != null ? item.getName() : "";
                String path = item.getPath() != null ? item.getPath() : filename;

                if (isSensitiveFile(filename, path)) {
                    risks.add(new Risk(
                        RiskCategory.SENSITIVE_FILES,
                        "Sensitive file found in repository: " + path,
                        determineSeverity(filename, path),
                        path
                    ));
                }
            }
        }

        return risks;
    }

    public boolean isSensitiveFile(String filename, String path) {
        String lowerName = filename.toLowerCase();
        String lowerPath = path.toLowerCase();

        // Environment files
        if (lowerName.equals(".env") || lowerName.startsWith(".env.") || lowerName.endsWith(".env")) {
            return true;
        }

        // SSH and private keys
        if (lowerName.equals("id_rsa") || lowerName.equals("id_dsa") || 
            lowerName.equals("id_ecdsa") || lowerName.equals("id_ed25519") ||
            lowerName.startsWith("id_rsa") || lowerName.startsWith("id_dsa")) {
            return true;
        }

        // Certificates & key stores
        if (lowerName.endsWith(".pem") || lowerName.endsWith(".key") || 
            lowerName.endsWith(".pkcs12") || lowerName.endsWith(".pfx") || 
            lowerName.endsWith(".p12") || lowerName.endsWith(".jks")) {
            return true;
        }

        // Secrets & config files
        if (lowerName.equals("secrets.yml") || lowerName.equals("secrets.yaml") ||
            lowerName.equals("config.json")) {
            return true;
        }

        // Specific sensitive paths
        if (lowerPath.contains(".aws/credentials") || lowerPath.contains(".aws/config") ||
            lowerPath.contains(".ssh/config") || lowerPath.contains(".ssh/id_rsa")) {
            return true;
        }

        return false;
    }

    private SeverityLevel determineSeverity(String filename, String path) {
        String lowerName = filename.toLowerCase();
        String lowerPath = path.toLowerCase();

        // High severity: active secrets, private keys, environment files, secrets yaml
        if (lowerName.contains(".env") ||
            lowerName.matches(".*\\.(pem|key|pfx|jks|p12)") ||
            lowerName.contains("id_rsa") || lowerName.contains("id_dsa") ||
            lowerName.contains("secrets.y") ||
            lowerPath.contains(".aws/credentials")) {
            return SeverityLevel.HIGH;
        }

        return SeverityLevel.MEDIUM;
    }
}
