package dev.simstoe.lupocloud.api.manager;

import dev.simstoe.lupocloud.api.models.CloudService;
import dev.simstoe.lupocloud.api.models.ServiceTask;
import dev.simstoe.lupocloud.api.models.ServiceType;

import java.util.List;
import java.util.Optional;

public interface ICloudManager {
    void startServer(String name, int port, String templateName);

    default void startServer(String name, int port) {
        startServer(name, port, null);
    }

    void startProxy(String name, int port, String templateName);

    default void startProxy(String name, int port) {
        startProxy(name, port, null);
    }

    void stopService(String name);
    boolean isRunning(String name);
    Optional<CloudService> service(String name);
    List<CloudService> activeServices();
    List<String> serviceLogs(String name);
    void sendCommand(String name, String command);
    void stopAll();

    List<String> templates();
    List<String> templates(ServiceType type);
    void saveTemplate(String templateName, String sourceService);
    void deleteTemplate(String templateName);

    List<String> tasks();
    Optional<ServiceTask> task(String name);
    void createTask(ServiceTask task);
    void saveTask(ServiceTask task);
    void deleteTask(String name);

    void startFromTask(String taskName);

    List<String> backups(String serviceName);
    void createBackup(String serviceName);
    void restoreBackup(String serviceName, String backupFileName);
    void deleteBackup(String serviceName, String backupFileName);
}
