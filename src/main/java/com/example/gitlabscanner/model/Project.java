package com.example.gitlabscanner.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private Long id;
    private String name;
    private String webUrl;
    private String description;
    private Boolean isPublic;
    private List<Risk> risks = new ArrayList<>();

    public SeverityLevel getMaxSeverity() {
        if (risks.isEmpty()) return null;
        return risks.stream()
            .map(Risk::getSeverity)
            .min((s1, s2) -> Integer.compare(s1.getOrder(), s2.getOrder()))
            .orElse(null);
    }
}
