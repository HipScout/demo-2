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
public class MissingMetadataScanner {
    private static final List<String> REQUIRED_FILES = List.of("README.md", "README.rst", "README", "LICENSE", "LICENSE.md");

    public List<Risk> scan(GitLabApiClient client, Long projectId) {
        List<Risk> risks = new ArrayList<>();
        List<GitLabTreeItemDTO> treeItems = client.getRepositoryTree(projectId, null);
        
        boolean hasReadme = treeItems.stream()
            .anyMatch(item -> item.getType().equals("blob") && 
                     (item.getName().toLowerCase().startsWith("readme") || 
                      item.getName().toLowerCase().equals("readme.md")));
        
        boolean hasLicense = treeItems.stream()
            .anyMatch(item -> item.getType().equals("blob") && 
                     (item.getName().toLowerCase().equals("license") || 
                      item.getName().toLowerCase().equals("license.md") ||
                      item.getName().toLowerCase().equals("copying")));
        
        if (!hasReadme) {
            risks.add(new Risk(
                RiskCategory.MISSING_METADATA,
                "Missing README.md file in repository root",
                SeverityLevel.LOW,
                "Repository root"
            ));
        }
        
        if (!hasLicense) {
            risks.add(new Risk(
                RiskCategory.MISSING_METADATA,
                "Missing LICENSE file in repository root",
                SeverityLevel.LOW,
                "Repository root"
            ));
        }
        
        return risks;
    }
}
