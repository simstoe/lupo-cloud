package dev.simstoe.lupocloud.api.models;

public enum ServiceType {
    PAPER("paper", "https://fill-data.papermc.io/v1/objects/3ec81e3ea50cc6090b94aab024491846a202702e8a874308a5d7510f6b3aa012/paper-26.2-111.jar"),
    VELOCITY("velocity", "https://fill-data.papermc.io/v1/objects/aebade8be3b15d7c3c61514a50ce857cbf78ee87bd32e8d16d2352c6ca3e472f/velocity-4.1.0-SNAPSHOT-16.jar");

    private final String projectId;
    private final String fallbackDownloadUrl;

    ServiceType(String projectId, String fallbackDownloadUrl) {
        this.projectId = projectId;
        this.fallbackDownloadUrl = fallbackDownloadUrl;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getFallbackDownloadUrl() {
        return fallbackDownloadUrl;
    }
}