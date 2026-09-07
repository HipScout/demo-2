package com.example.gitlabscanner.report;

import com.example.gitlabscanner.model.Project;
import com.example.gitlabscanner.model.Risk;
import com.example.gitlabscanner.model.ScanResult;
import com.example.gitlabscanner.model.SeverityLevel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ReportGenerator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String generateCliReport(ScanResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n╔════════════════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║ GitLab Repository Risk Scan Report %-35s║\n", ""));
        sb.append("╠════════════════════════════════════════════════════════════════════╣\n");
        
        String scanTarget = result.getUsername() != null ? 
            "User: " + result.getUsername() : 
            "Group: " + result.getGroupName();
        sb.append(String.format("║ %-66s ║\n", scanTarget));
        sb.append(String.format("║ Scan Time: %-56s ║\n", result.getScanTime()));
        sb.append(String.format("║ Total Projects: %d | Projects with Risks: %d %-36s ║\n", 
            result.getTotalProjects(), result.getProjectsWithRisks(), ""));
        
        sb.append("╠════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ Project Name               │ Issues                │ Severity     ║\n");
        sb.append("╠════════════════════════════════════════════════════════════════════╣\n");
        
        for (Project project : result.getProjectsScanned()) {
            if (!project.getRisks().isEmpty()) {
                String projectName = truncate(project.getName(), 25);
                String issues = truncate(formatIssues(project), 20);
                String severity = project.getMaxSeverity() != null ? 
                    project.getMaxSeverity().toString() : "NONE";
                
                sb.append(String.format("║ %-25s │ %-20s │ %-13s ║\n", 
                    projectName, issues, severity));
            }
        }
        
        sb.append("╠════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ HIGH: %d | MEDIUM: %d | LOW: %d %-37s ║\n",
            result.getHighRiskCount(),
            result.getMediumRiskCount(),
            result.getLowRiskCount(),
            ""));
        sb.append("╚════════════════════════════════════════════════════════════════════╝\n");
        
        return sb.toString();
    }

    public String generateDetailedCliReport(ScanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateCliReport(result));
        sb.append("\n\n═══════════════════════════════════════════════════════════════════\n");
        sb.append("DETAILED FINDINGS\n");
        sb.append("═══════════════════════════════════════════════════════════════════\n\n");
        
        for (Project project : result.getProjectsScanned()) {
            if (!project.getRisks().isEmpty()) {
                sb.append(String.format("📦 Project: %s\n", project.getName()));
                sb.append(String.format("   URL: %s\n\n", project.getWebUrl()));
                
                for (Risk risk : project.getRisks()) {
                    String severity = String.format("[%s]", risk.getSeverity());
                    sb.append(String.format("   %s %s\n", severity, risk.getCategory().getDisplayName()));
                    sb.append(String.format("       └─ %s\n", risk.getDescription()));
                    sb.append(String.format("       └─ Evidence: %s\n\n", risk.getEvidence()));
                }
            }
        }
        
        return sb.toString();
    }

    public String generateJsonReport(ScanResult result) {
        return gson.toJson(result);
    }

    private String formatIssues(Project project) {
        return project.getRisks().stream()
            .map(r -> r.getCategory().getDisplayName())
            .distinct()
            .limit(2)
            .collect(Collectors.joining(", "));
    }

    private String truncate(String str, int length) {
        if (str.length() > length) {
            return str.substring(0, length - 3) + "...";
        }
        return String.format("%-" + length + "s", str);
    }
}
