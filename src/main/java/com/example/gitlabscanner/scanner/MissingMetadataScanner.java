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

    public List<Risk> scan(GitLabApiClient client, Long projectId) {
        List<GitLabTreeItemDTO> treeItems = client.getRepositoryTree(projectId, null);
        return scan(treeItems);
    }

    public List<Risk> scan(List<GitLabTreeItemDTO> treeItems) {
        List<Risk> risks = new ArrayList<>();
        if (treeItems == null) {
            treeItems = List.of();
        }
        
        boolean hasReadme = treeItems.stream()
            .anyMatch(item -> "blob".equalsIgnoreCase(item.getType()) && 
                     (item.getName().toLowerCase().startsWith("readme") || 
                      item.getName().toLowerCase().equals("readme.md") ||
                      item.getName().toLowerCase().equals("readme.rst") ||
                      item.getName().toLowerCase().equals("readme.txt")));
        
        boolean hasLicense = treeItems.stream()
            .anyMatch(item -> "blob".equalsIgnoreCase(item.getType()) && 
                     (item.getName().toLowerCase().startsWith("license") || 
                      item.getName().toLowerCase().startsWith("licence") ||
                      item.getName().toLowerCase().startsWith("copying")));
        
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
