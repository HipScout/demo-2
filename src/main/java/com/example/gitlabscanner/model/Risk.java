package com.example.gitlabscanner.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Risk {
    private RiskCategory category;
    private String description;
    private SeverityLevel severity;
    private String evidence;
}
