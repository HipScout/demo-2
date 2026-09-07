package com.example.gitlabscanner.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanResult {
    private String username;
    private String groupName;
    private LocalDateTime scanTime;
    private List<Project> projectsScanned = new ArrayList<>();
    private int totalProjects;
    private int projectsWithRisks;
    private SeverityLevel highestSeverity;

    public SeverityLevel calculateHighestSeverity() {
        return projectsScanned.stream()
            .flatMap(p -> p.getRisks().stream())
            .map(Risk::getSeverity)
            .min((s1, s2) -> Integer.compare(s1.getOrder(), s2.getOrder()))
            .orElse(null);
    }

    public int getHighRiskCount() {
        return (int) projectsScanned.stream()
            .flatMap(p -> p.getRisks().stream())
            .filter(r -> r.getSeverity() == SeverityLevel.HIGH)
            .count();
    }

    public int getMediumRiskCount() {
        return (int) projectsScanned.stream()
            .flatMap(p -> p.getRisks().stream())
            .filter(r -> r.getSeverity() == SeverityLevel.MEDIUM)
            .count();
    }

    public int getLowRiskCount() {
        return (int) projectsScanned.stream()
            .flatMap(p -> p.getRisks().stream())
            .filter(r -> r.getSeverity() == SeverityLevel.LOW)
            .count();
    }
}
