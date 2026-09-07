package com.example.gitlabscanner.model;

public enum SeverityLevel {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int order;

    SeverityLevel(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
