package com.example.gitlabscanner.model;

public enum RiskCategory {
    SENSITIVE_FILES("Sensitive Files"),
    EXPOSED_SECRETS("Exposed Secrets"),
    MISSING_METADATA("Missing Metadata");

    private final String displayName;

    RiskCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
