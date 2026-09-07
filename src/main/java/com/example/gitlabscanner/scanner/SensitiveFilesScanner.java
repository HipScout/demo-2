package com.example.gitlabscanner.scanner;

import com.example.gitlabscanner.client.GitLabApiClient;
import com.example.gitlabscanner.dto.GitLabTreeItemDTO;
import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.RiskCategory;
import com.example.gitlabscanner.model.SeverityLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SensitiveFilesScanner {
    private static final Set<String> SENSITIVE_FILES = Set.of(
        ".env", ".env.local", ".env.*.local",
        ".pem", "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519",
        "config.json", "secrets.yml", "secrets.yaml",
        ".aws/credentials", ".aws/config",
        ".ssh/config", "keystore.jks", "certificate.pfx"
    );

    public List<Risk> scan(GitLabApiClient client, Long projectId) {
        List<Risk> risks = new ArrayList<>();
        List<GitLabTreeItemDTO> treeItems = client.getRepositoryTree(projectId, null);
        
        for (GitLabTreeItemDTO item : treeItems) {
            if (item.getType().equals("blob") && isSensitiveFile(item.getName())) {
                risks.add(new Risk(
                    RiskCategory.SENSITIVE_FILES,
                    "Sensitive file found in repository: " + item.getName(),
                    getSeverity(item.getName()),
                    item.getPath()
                ));
            }
        }
        
        return risks;
    }

    private boolean isSensitiveFile(String filename) {
        return SENSITIVE_FILES.stream()
            .anyMatch(pattern -> {
                if (pattern.contains("*")) {
                    String regex = pattern.replace(".", "\\.").replace("*", ".*");
                    return filename.matches(regex);
                }
                return filename.equals(pattern);
            });
    }

    private SeverityLevel getSeverity(String filename) {
        if (filename.matches(".*\\.(pem|key|id_rsa|id_dsa|id_ecdsa|id_ed25519)")) {
            return SeverityLevel.HIGH;
        }
        return SeverityLevel.MEDIUM;
    }
}
