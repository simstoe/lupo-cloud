package dev.simstoe.lupocloud.node.registry;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.CloudService;
import dev.simstoe.lupocloud.api.models.ServiceTask;
import dev.simstoe.lupocloud.api.models.ServiceType;
import dev.simstoe.lupocloud.node.registry.backup.BackupManager;
import dev.simstoe.lupocloud.node.registry.config.ServiceConfigHandler;
import dev.simstoe.lupocloud.node.registry.config.ServiceDownloader;
import dev.simstoe.lupocloud.node.registry.task.TaskManager;
import dev.simstoe.lupocloud.node.registry.template.TemplateManager;
import dev.simstoe.lupocloud.node.registry.util.DirectoryUtil;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class CloudManagerImpl implements ICloudManager {
    private static final int MAX_LOG_HISTORY = 100;

    private static final int MAX_RESTART_ATTEMPTS = 3;
    private static final long RESTART_DELAY_SECONDS = 5;
    private static final long RESTART_COUNTER_RESET_SECONDS = 60;

    private static final int DEFAULT_PAPER_MIN_MEMORY_MB = 1024;
    private static final int DEFAULT_PAPER_MAX_MEMORY_MB = 2048;
    private static final int DEFAULT_VELOCITY_MIN_MEMORY_MB = 512;
    private static final int DEFAULT_VELOCITY_MAX_MEMORY_MB = 1024;

    private static final File TEMP_DIR = new File("temp");

    private final Map<String, ManagedInstance> instances = new ConcurrentHashMap<>();

    private final Map<String, Integer> restartAttempts = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(CloudManagerImpl::newDaemonThread);

    private final TaskManager taskManager = new TaskManager();
    private final ServiceConfigHandler configHandler = new ServiceConfigHandler(taskManager);
    private final ServiceDownloader downloader = new ServiceDownloader();
    private final TemplateManager templateManager = new TemplateManager();
    private final PortAllocator portAllocator = new PortAllocator();
    private final BackupManager backupManager = new BackupManager();

    private final boolean tasksetAvailable = isTasksetAvailable();

    private record LaunchSpec(String name, int port, String templateName, int minMemoryMB, int maxMemoryMB,
                               Integer cpuCoreLimit, String taskName, File serviceDir, boolean ephemeral) {}

    private static final class ManagedInstance {
        volatile CloudService info;
        final Process process;
        final List<String> logs = Collections.synchronizedList(new ArrayList<>());
        final Runnable restartAction;
        final File serviceDir;
        final boolean ephemeral;
        volatile boolean intentionalStop = false;

        ManagedInstance(CloudService info, Process process, Runnable restartAction, File serviceDir, boolean ephemeral) {
            this.info = info;
            this.process = process;
            this.restartAction = restartAction;
            this.serviceDir = serviceDir;
            this.ephemeral = ephemeral;
        }
    }

    public CloudManagerImpl() {
        loadExistingServices();
    }

    private static Thread newDaemonThread(Runnable r) {
        Thread thread = new Thread(r, "cloud-worker");
        thread.setDaemon(true);
        return thread;
    }

    private String formatName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static boolean isTasksetAvailable() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")) return false;
        try {
            Process process = new ProcessBuilder("which", "taskset").redirectErrorStream(true).start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> buildJavaCommand(String jarFileName, int minMemoryMB, int maxMemoryMB, Integer cpuCoreLimit, String... extraArgs) {
        List<String> command = new ArrayList<>();
        if (cpuCoreLimit != null && cpuCoreLimit > 0) {
            if (tasksetAvailable) {
                command.add("taskset");
                command.add("-c");
                command.add("0-" + (cpuCoreLimit - 1));
            } else {
                CloudLogger.info("CPU core limit requested but 'taskset' is unavailable on this platform - starting without a CPU limit.");
            }
        }
        command.add("java");
        command.add("-Xms" + minMemoryMB + "M");
        command.add("-Xmx" + maxMemoryMB + "M");
        command.add("-jar");
        command.add(jarFileName);
        command.addAll(Arrays.asList(extraArgs));
        return command;
    }

    @Override
    public boolean isRunning(String name) {
        String key = formatName(name);
        ManagedInstance instance = instances.get(key);
        if (instance == null) return false;
        if (!instance.process.isAlive()) {
            cleanup(key);
            return false;
        }
        return true;
    }

    private void cleanup(String key) {
        instances.remove(key);
    }

    public void loadExistingServices() {
        cleanupTempDirectory();
        CloudLogger.info("Scanning for existing services...");

        List<CompletableFuture<Void>> bootTasks = new ArrayList<>();

        File serversDir = new File("servers");
        File[] serverFolders = serversDir.isDirectory() ? serversDir.listFiles(File::isDirectory) : null;
        if (serverFolders != null) {
            for (File folder : serverFolders) {
                if (!new File(folder, "paper.jar").exists()) continue;
                String name = folder.getName();
                if (!configHandler.shouldAutostart(folder)) {
                    CloudLogger.info("Skipping '" + name + "' - its task has autostart disabled.");
                    continue;
                }
                int port = configHandler.extractServerPort(folder);
                CloudLogger.info("Auto-starting existing paper server: " + name + " (Port: " + port + ")");
                LaunchSpec spec = new LaunchSpec(name, port, null, DEFAULT_PAPER_MIN_MEMORY_MB, DEFAULT_PAPER_MAX_MEMORY_MB, null, null, folder, false);
                bootTasks.add(CompletableFuture.runAsync(() -> startServerInternal(spec, true), executor));
            }
        }

        File proxiesDir = new File("proxies");
        File[] proxyFolders = proxiesDir.isDirectory() ? proxiesDir.listFiles(File::isDirectory) : null;
        if (proxyFolders != null) {
            for (File folder : proxyFolders) {
                if (!new File(folder, "velocity.jar").exists()) continue;
                String name = folder.getName();
                if (!configHandler.shouldAutostart(folder)) {
                    CloudLogger.info("Skipping '" + name + "' - its task has autostart disabled.");
                    continue;
                }
                int port = configHandler.extractProxyPort(folder);
                CloudLogger.info("Auto-starting existing proxy: " + name + " (Port: " + port + ")");
                LaunchSpec spec = new LaunchSpec(name, port, null, DEFAULT_VELOCITY_MIN_MEMORY_MB, DEFAULT_VELOCITY_MAX_MEMORY_MB, null, null, folder, false);
                bootTasks.add(CompletableFuture.runAsync(() -> startProxyInternal(spec, true), executor));
            }
        }

        CompletableFuture.allOf(bootTasks.toArray(new CompletableFuture[0])).join();

        configHandler.updateAllProxyConfigs();

        if (bootTasks.isEmpty()) {
            CloudLogger.info("No existing services found.");
        } else {
            CloudLogger.success("Auto-started " + bootTasks.size() + " existing service(s).");
        }
    }

    private void cleanupTempDirectory() {
        if (!TEMP_DIR.exists()) return;
        try {
            DirectoryUtil.deleteRecursively(TEMP_DIR.toPath());
        } catch (IOException e) {
            CloudLogger.error("Could not clean up the temp/ directory: " + e.getMessage());
        }
    }

    @Override
    public void startServer(String name, int port, String templateName) {
        File serviceDir = new File("servers", name);
        LaunchSpec spec = new LaunchSpec(name, port, templateName, DEFAULT_PAPER_MIN_MEMORY_MB, DEFAULT_PAPER_MAX_MEMORY_MB, null, null, serviceDir, false);
        startServerInternal(spec, false);
    }

    private void startServerInternal(LaunchSpec spec, boolean duringBoot) {
        String key = formatName(spec.name());
        if (isRunning(spec.name())) {
            CloudLogger.error("Server '" + spec.name() + "' is already running!");
            return;
        }

        File serviceDir = spec.serviceDir();
        boolean isNew = !serviceDir.exists();
        if (isNew) serviceDir.mkdirs();

        var paperJar = new File(serviceDir, "paper.jar");
        if (!paperJar.exists()) {
            CloudLogger.info("No paper.jar found in server '" + spec.name() + "'.");
            boolean downloaded = downloader.provide(ServiceType.PAPER, paperJar, executor).join();
            if (!downloaded) return;
        }

        if (isNew && spec.templateName() != null && !spec.templateName().isBlank()) {
            applyTemplateOrWarn(spec.templateName(), serviceDir);
        }
        if (isNew && spec.taskName() != null) {
            configHandler.writeTaskMarker(serviceDir, spec.taskName());
        }

        configHandler.acceptEula(serviceDir);
        configHandler.configurePaperForVelocity(serviceDir);
        configHandler.updateServerPort(serviceDir, spec.port());
        if (!duringBoot) configHandler.updateAllProxyConfigs();

        try {
            CloudLogger.info("Starting paper server '" + spec.name() + "' on port " + spec.port() + "...");
            ProcessBuilder builder = new ProcessBuilder(buildJavaCommand("paper.jar", spec.minMemoryMB(), spec.maxMemoryMB(), spec.cpuCoreLimit(), "nogui"));
            builder.directory(serviceDir);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            Runnable restartAction = () -> startServerInternal(spec, false);
            registerProcess(key, spec.name(), ServiceType.PAPER, spec.port(), process, restartAction, serviceDir, spec.ephemeral());
            CloudLogger.success("Server '" + spec.name() + "' started successfully!");

        } catch (Exception e) {
            CloudLogger.error("Failed to start '" + spec.name() + "': " + e.getMessage());
        }
    }

    @Override
    public void startProxy(String name, int port, String templateName) {
        File serviceDir = new File("proxies", name);
        LaunchSpec spec = new LaunchSpec(name, port, templateName, DEFAULT_VELOCITY_MIN_MEMORY_MB, DEFAULT_VELOCITY_MAX_MEMORY_MB, null, null, serviceDir, false);
        startProxyInternal(spec, false);
    }

    private void startProxyInternal(LaunchSpec spec, boolean duringBoot) {
        String key = formatName(spec.name());
        if (isRunning(spec.name())) {
            CloudLogger.error("Proxy '" + spec.name() + "' is already running!");
            return;
        }

        File serviceDir = spec.serviceDir();
        boolean isNew = !serviceDir.exists();
        if (isNew) serviceDir.mkdirs();

        var velocityJar = new File(serviceDir, "velocity.jar");
        if (!velocityJar.exists()) {
            CloudLogger.info("No velocity.jar found in proxy '" + spec.name() + "'.");
            boolean downloaded = downloader.provide(ServiceType.VELOCITY, velocityJar, executor).join();
            if (!downloaded) return;
        }

        if (isNew && spec.templateName() != null && !spec.templateName().isBlank()) {
            applyTemplateOrWarn(spec.templateName(), serviceDir);
        }
        if (isNew && spec.taskName() != null) {
            configHandler.writeTaskMarker(serviceDir, spec.taskName());
        }

        String secret;
        try {
            secret = configHandler.generateOrReadSecret(serviceDir);
        } catch (IOException e) {
            CloudLogger.error("Failed to start proxy '" + spec.name() + "': forwarding secret unavailable (" + e.getMessage() + ")");
            return;
        }
        configHandler.generateVelocityConfig(serviceDir, spec.port(), secret);

        try {
            CloudLogger.info("Starting Velocity proxy '" + spec.name() + "' on port " + spec.port() + "...");
            ProcessBuilder builder = new ProcessBuilder(buildJavaCommand("velocity.jar", spec.minMemoryMB(), spec.maxMemoryMB(), spec.cpuCoreLimit()));
            builder.directory(serviceDir);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            Runnable restartAction = () -> startProxyInternal(spec, false);
            registerProcess(key, spec.name(), ServiceType.VELOCITY, spec.port(), process, restartAction, serviceDir, spec.ephemeral());
            CloudLogger.success("Velocity proxy '" + spec.name() + "' started successfully!");

        } catch (Exception e) {
            CloudLogger.error("Failed to start proxy '" + spec.name() + "': " + e.getMessage());
        }
    }

    @Override
    public void startFromTask(String taskName) {
        Optional<ServiceTask> maybeTask = taskManager.getTask(taskName);
        if (maybeTask.isEmpty()) {
            CloudLogger.error("Task '" + taskName + "' does not exist.");
            return;
        }
        ServiceTask task = maybeTask.get();

        String name = nextInstanceName(task.name());
        int port;
        try {
            port = portAllocator.allocate(task.startPort(), activeServices());

            CloudLogger.error("DER PORT: " + port);
        } catch (IllegalStateException e) {
            CloudLogger.error("Could not allocate a port for task '" + taskName + "': " + e.getMessage());
            return;
        }

        boolean ephemeral = !task.staticService();
        File serviceDir = ephemeral
                ? new File(TEMP_DIR, UUID.randomUUID().toString())
                : new File(task.type() == ServiceType.VELOCITY ? "proxies" : "servers", name);

        LaunchSpec spec = new LaunchSpec(name, port, task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), task.name(), serviceDir, ephemeral);

        if (task.type() == ServiceType.VELOCITY) {
            startProxyInternal(spec, false);
        } else {
            startServerInternal(spec, false);
        }
    }

    private String nextInstanceName(String taskName) {
        String base = formatName(taskName);
        int index = 1;
        while (instances.containsKey(base + "-" + index)
                || new File("servers", base + "-" + index).exists()
                || new File("proxies", base + "-" + index).exists()) {
            index++;
        }
        return base + "-" + index;
    }

    private void applyTemplateOrWarn(String templateName, File targetDir) {
        try {
            templateManager.applyTemplate(templateName, targetDir);
            CloudLogger.success("Template '" + templateName + "' applied.");
        } catch (IOException e) {
            CloudLogger.error("Could not apply template '" + templateName + "': " + e.getMessage());
        }
    }

    private void registerProcess(String key, String name, ServiceType type, int port, Process process, Runnable restartAction, File serviceDir, boolean ephemeral) {
        var instance = new ManagedInstance(CloudService.create(name, type, port).running(true), process, restartAction, serviceDir, ephemeral);
        instances.put(key, instance);

        Thread logReaderThread = new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean readyReported = false;
                while ((line = reader.readLine()) != null) {
                    synchronized (instance.logs) {
                        instance.logs.add(line);
                        if (instance.logs.size() > MAX_LOG_HISTORY) instance.logs.remove(0);
                    }
                    if (!readyReported && line.contains("Done (") && line.contains("For help, type \"help\"")) {
                        readyReported = true;
                        CloudLogger.success("Instance '" + name + "' is ready to join!");
                    }
                }
            } catch (Exception ignored) {}
        }, "LogReader-" + name);
        logReaderThread.setDaemon(true);
        logReaderThread.start();

        process.onExit().thenAccept(p -> {
            cleanup(key);
            CloudLogger.info("Instance '" + name + "' has stopped (Exit Code: " + p.exitValue() + ").");
            if (instance.intentionalStop) {
                restartAttempts.remove(key);
                deleteIfEphemeral(instance);
            } else if (!scheduleRestart(key, name, instance.restartAction)) {
                deleteIfEphemeral(instance);
            }
        });

        CompletableFuture.delayedExecutor(RESTART_COUNTER_RESET_SECONDS, TimeUnit.SECONDS, executor)
                .execute(() -> {
                    if (process.isAlive()) restartAttempts.remove(key);
                });
    }

    private void deleteIfEphemeral(ManagedInstance instance) {
        if (!instance.ephemeral) return;
        try {
            DirectoryUtil.deleteRecursively(instance.serviceDir.toPath());
        } catch (IOException e) {
            CloudLogger.error("Could not clean up ephemeral working directory '" + instance.serviceDir + "': " + e.getMessage());
        }
    }

    private boolean scheduleRestart(String key, String name, Runnable restartAction) {
        int attempt = restartAttempts.merge(key, 1, Integer::sum);
        if (attempt > MAX_RESTART_ATTEMPTS) {
            CloudLogger.error("Instance '" + name + "' crashed repeatedly (" + MAX_RESTART_ATTEMPTS + " failed attempts) - giving up on automatic restart.");
            restartAttempts.remove(key);
            return false;
        }

        CloudLogger.info("Instance '" + name + "' crashed unexpectedly - restarting in " + RESTART_DELAY_SECONDS
                + "s (attempt " + attempt + "/" + MAX_RESTART_ATTEMPTS + ")...");
        CompletableFuture.delayedExecutor(RESTART_DELAY_SECONDS, TimeUnit.SECONDS, executor).execute(restartAction);
        return true;
    }

    @Override
    public void stopService(String name) {
        String key = formatName(name);
        ManagedInstance instance = instances.get(key);
        if (instance == null) {
            CloudLogger.error("No running instance found with the name '" + name + "'.");
            return;
        }

        CloudLogger.info("Stopping '" + name + "'...");
        instance.intentionalStop = true;
        instance.process.destroy();
        cleanup(key);
        restartAttempts.remove(key);

        if (instance.info.type() == ServiceType.PAPER) {
            configHandler.updateAllProxyConfigs();
        }
    }

    @Override
    public Optional<CloudService> service(String name) {
        return Optional.ofNullable(instances.get(formatName(name))).map(instance -> instance.info);
    }

    @Override
    public List<CloudService> activeServices() {
        return instances.values().stream().map(instance -> instance.info).toList();
    }

    @Override
    public List<String> serviceLogs(String name) {
        ManagedInstance instance = instances.get(formatName(name));
        if (instance == null) return Collections.emptyList();
        synchronized (instance.logs) {
            return new ArrayList<>(instance.logs);
        }
    }

    @Override
    public void sendCommand(String name, String command) {
        ManagedInstance instance = instances.get(formatName(name));
        if (instance == null) {
            CloudLogger.error("Instance '" + name + "' is not running.");
            return;
        }
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(instance.process.getOutputStream()));
            writer.write(command + "\n");
            writer.flush();
        } catch (Exception e) {
            CloudLogger.error("Failed to send command: " + e.getMessage());
        }
    }

    @Override
    public void stopAll() {
        if (!instances.isEmpty()) {
            CloudLogger.info("Shutting down all active instances...");
            for (ManagedInstance instance : instances.values()) {
                instance.intentionalStop = true;
                try {
                    instance.process.destroy();
                    if (!instance.process.waitFor(3, TimeUnit.SECONDS)) instance.process.destroyForcibly();
                } catch (Exception e) {
                    instance.process.destroyForcibly();
                }
            }
            instances.clear();
            restartAttempts.clear();
            CloudLogger.success("All instances have been stopped.");
        }
        executor.shutdownNow();
    }

    @Override
    public List<String> templates() {
        return templateManager.listTemplates();
    }

    @Override
    public List<String> templates(ServiceType type) {
        return templateManager.listTemplates().stream()
                .filter(name -> templateManager.typeOf(name).map(type::equals).orElse(false))
                .toList();
    }

    @Override
    public void saveTemplate(String templateName, String sourceService) {
        File sourceDir = resolveServiceDirectory(sourceService);
        if (sourceDir == null) {
            CloudLogger.error("No server or proxy folder named '" + sourceService + "' found.");
            return;
        }

        ServiceType type = detectType(sourceDir);
        if (type == null) {
            CloudLogger.error("Could not determine the type of '" + sourceService + "' (no paper.jar/velocity.jar found).");
            return;
        }

        try {
            templateManager.saveTemplate(templateName, sourceDir, type);
            CloudLogger.success("Template '" + templateName + "' saved from '" + sourceService + "'.");
        } catch (IOException e) {
            CloudLogger.error("Could not save template '" + templateName + "': " + e.getMessage());
        }
    }

    @Override
    public void deleteTemplate(String templateName) {
        try {
            templateManager.deleteTemplate(templateName);
            CloudLogger.success("Template '" + templateName + "' deleted.");
        } catch (IOException e) {
            CloudLogger.error("Could not delete template '" + templateName + "': " + e.getMessage());
        }
    }

    @Override
    public List<String> tasks() {
        return taskManager.listTasks();
    }

    @Override
    public Optional<ServiceTask> task(String name) {
        return taskManager.getTask(name);
    }

    @Override
    public void createTask(ServiceTask task) {
        try {
            taskManager.createTask(task);
            CloudLogger.success("Task '" + task.name() + "' created.");
        } catch (IOException e) {
            CloudLogger.error("Could not create task '" + task.name() + "': " + e.getMessage());
        }
    }

    @Override
    public void saveTask(ServiceTask task) {
        try {
            taskManager.saveTask(task);
            CloudLogger.success("Task '" + task.name() + "' saved.");
        } catch (IOException e) {
            CloudLogger.error("Could not save task '" + task.name() + "': " + e.getMessage());
        }
    }

    @Override
    public void deleteTask(String name) {
        try {
            taskManager.deleteTask(name);
            CloudLogger.success("Task '" + name + "' deleted.");
        } catch (IOException e) {
            CloudLogger.error("Could not delete task '" + name + "': " + e.getMessage());
        }
    }

    @Override
    public List<String> backups(String serviceName) {
        return backupManager.listBackups(serviceName);
    }

    @Override
    public void createBackup(String serviceName) {
        File sourceDir = resolveServiceDirectory(serviceName);
        if (sourceDir == null) {
            CloudLogger.error("No server or proxy folder named '" + serviceName + "' found.");
            return;
        }
        try {
            File backupFile = backupManager.createBackup(serviceName, sourceDir);
            CloudLogger.success("Backup '" + backupFile.getName() + "' created for '" + serviceName + "'.");
        } catch (IOException e) {
            CloudLogger.error("Could not create backup for '" + serviceName + "': " + e.getMessage());
        }
    }

    @Override
    public void restoreBackup(String serviceName, String backupFileName) {
        if (isRunning(serviceName)) {
            CloudLogger.error("Stop '" + serviceName + "' before restoring a backup.");
            return;
        }
        File targetDir = resolveServiceDirectory(serviceName);
        if (targetDir == null) {
            CloudLogger.error("No server or proxy folder named '" + serviceName + "' found.");
            return;
        }
        try {
            backupManager.restoreBackup(serviceName, backupFileName, targetDir);
            CloudLogger.success("Backup '" + backupFileName + "' restored into '" + serviceName + "'.");
        } catch (IOException e) {
            CloudLogger.error("Could not restore backup '" + backupFileName + "' for '" + serviceName + "': " + e.getMessage());
        }
    }

    @Override
    public void deleteBackup(String serviceName, String backupFileName) {
        try {
            backupManager.deleteBackup(serviceName, backupFileName);
            CloudLogger.success("Backup '" + backupFileName + "' deleted for '" + serviceName + "'.");
        } catch (IOException e) {
            CloudLogger.error("Could not delete backup '" + backupFileName + "' for '" + serviceName + "': " + e.getMessage());
        }
    }

    private File resolveServiceDirectory(String name) {
        File serverDir = new File("servers", name);
        if (serverDir.exists()) return serverDir;
        File proxyDir = new File("proxies", name);
        if (proxyDir.exists()) return proxyDir;
        return null;
    }

    private ServiceType detectType(File dir) {
        if (new File(dir, "paper.jar").exists()) return ServiceType.PAPER;
        if (new File(dir, "velocity.jar").exists()) return ServiceType.VELOCITY;
        return null;
    }
}
