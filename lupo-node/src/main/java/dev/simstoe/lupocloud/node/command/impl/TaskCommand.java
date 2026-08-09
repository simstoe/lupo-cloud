package dev.simstoe.lupocloud.node.command.impl;

import dev.simstoe.lupocloud.api.logging.CloudLogger;
import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.ServiceTask;
import dev.simstoe.lupocloud.api.models.ServiceType;
import dev.simstoe.lupocloud.node.command.ICommand;
import org.jline.terminal.Terminal;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TaskCommand implements ICommand {
    private static final List<String> EDITABLE_FIELDS = List.of(
            "template", "minmemory", "maxmemory", "cpucorelimit", "startport", "minonline", "maxonline", "static",
            "autostart", "group", "proxygroups");

    private final ICloudManager cloudManager;

    public TaskCommand(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @Override
    public String name() {
        return "task";
    }

    @Override
    public String[] aliases() {
        return new String[] { "tasks" };
    }

    @Override
    public String description() {
        return "Manage service tasks (Usage: task create|list|info|edit|delete)";
    }

    @Override
    public void execute(Terminal terminal, String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> createTask(args);
            case "list" -> listTasks();
            case "info" -> showTask(args);
            case "edit" -> editTask(args);
            case "delete" -> deleteTask(args);
            default -> printUsage();
        }

        terminal.flush();
    }

    private void createTask(String[] args) {
        if (args.length < 5) {
            CloudLogger.error("Usage: task create <name> <paper|proxy> <startPort> [templateName]");
            return;
        }

        String name = args[2];
        String typeInput = args[3].toLowerCase(Locale.ROOT);
        ServiceType type = (typeInput.equals("proxy") || typeInput.equals("velocity")) ? ServiceType.VELOCITY : ServiceType.PAPER;

        int startPort;
        try {
            startPort = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            CloudLogger.error("'" + args[4] + "' is not a valid port number.");
            return;
        }

        ServiceTask task = ServiceTask.create(name, type, startPort);
        if (args.length >= 6) {
            task = withTemplate(task, args[5]);
        }

        cloudManager.createTask(task);
    }

    private void listTasks() {
        List<String> tasks = cloudManager.tasks();
        if (tasks.isEmpty()) {
            CloudLogger.info("No tasks available yet. Create one with 'task create <name> <paper|proxy> <startPort>'.");
        } else {
            CloudLogger.info("Available tasks:");
            tasks.forEach(t -> CloudLogger.info(" - &b" + t));
        }
    }

    private void showTask(String[] args) {
        if (args.length < 3) {
            CloudLogger.error("Usage: task info <name>");
            return;
        }
        cloudManager.task(args[2]).ifPresentOrElse(
                task -> CloudLogger.info(("&7name: &b%s&r, type: &b%s&r, template: &b%s&r, memory: &b%d-%dMB&r, "
                        + "cpuCoreLimit: &b%s&r, startPort: &b%d&r, minOnline: &b%d&r, maxOnline: &b%d&r, static: &b%s&r, "
                        + "autostart: &b%s&r, group: &b%s&r, proxyGroups: &b%s")
                        .formatted(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                                task.cpuCoreLimit() == null ? "-" : task.cpuCoreLimit(), task.startPort(),
                                task.minOnlineCount(), task.maxOnlineCount(), task.staticService(), task.autostart(),
                                task.group() == null ? "-" : task.group(),
                                (task.proxyGroups() == null || task.proxyGroups().isEmpty()) ? "all" : task.proxyGroups())),
                () -> CloudLogger.error("Unknown task '" + args[2] + "'.")
        );
    }

    private void editTask(String[] args) {
        if (args.length < 5) {
            CloudLogger.error("Usage: task edit <name> <" + String.join("|", EDITABLE_FIELDS) + "> <value>");
            return;
        }

        var existing = cloudManager.task(args[2]);
        if (existing.isEmpty()) {
            CloudLogger.error("Unknown task '" + args[2] + "'.");
            return;
        }

        ServiceTask task = existing.get();
        String field = args[3].toLowerCase(Locale.ROOT);
        String value = args[4];

        try {
            ServiceTask updated = switch (field) {
                case "template" -> withTemplate(task, value.equalsIgnoreCase("none") ? null : value);
                case "minmemory" -> withMemory(task, Integer.parseInt(value), task.maxMemoryMB());
                case "maxmemory" -> withMemory(task, task.minMemoryMB(), Integer.parseInt(value));
                case "cpucorelimit" -> withCpuCoreLimit(task, value.equalsIgnoreCase("none") ? null : Integer.parseInt(value));
                case "startport" -> withStartPort(task, Integer.parseInt(value));
                case "minonline" -> withOnlineCounts(task, Integer.parseInt(value), task.maxOnlineCount());
                case "maxonline" -> withOnlineCounts(task, task.minOnlineCount(), Integer.parseInt(value));
                case "static" -> withStatic(task, Boolean.parseBoolean(value));
                case "autostart" -> withAutostart(task, Boolean.parseBoolean(value));
                case "group" -> withGroup(task, value.equalsIgnoreCase("none") ? null : value);
                case "proxygroups" -> withProxyGroups(task, value.equalsIgnoreCase("none") ? null : Arrays.asList(value.split(",")));
                default -> null;
            };

            if (updated == null) {
                CloudLogger.error("Unknown field '" + field + "'. Editable fields: " + String.join(", ", EDITABLE_FIELDS));
                return;
            }

            cloudManager.saveTask(updated);
        } catch (NumberFormatException e) {
            CloudLogger.error("'" + value + "' is not a valid number.");
        }
    }

    private void deleteTask(String[] args) {
        if (args.length < 3) {
            CloudLogger.error("Usage: task delete <name>");
            return;
        }
        cloudManager.deleteTask(args[2]);
    }

    private ServiceTask withTemplate(ServiceTask task, String templateName) {
        return new ServiceTask(task.name(), task.type(), templateName, task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), task.startPort(), task.minOnlineCount(), task.maxOnlineCount(), task.staticService(),
                task.autostart(), task.group(), task.proxyGroups());
    }

    private ServiceTask withMemory(ServiceTask task, int minMemoryMB, int maxMemoryMB) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), minMemoryMB, maxMemoryMB,
                task.cpuCoreLimit(), task.startPort(), task.minOnlineCount(), task.maxOnlineCount(), task.staticService(),
                task.autostart(), task.group(), task.proxyGroups());
    }

    private ServiceTask withCpuCoreLimit(ServiceTask task, Integer cpuCoreLimit) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                cpuCoreLimit, task.startPort(), task.minOnlineCount(), task.maxOnlineCount(), task.staticService(),
                task.autostart(), task.group(), task.proxyGroups());
    }

    private ServiceTask withStartPort(ServiceTask task, int startPort) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), startPort, task.minOnlineCount(), task.maxOnlineCount(), task.staticService(),
                task.autostart(), task.group(), task.proxyGroups());
    }

    private ServiceTask withOnlineCounts(ServiceTask task, int minOnlineCount, int maxOnlineCount) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), task.startPort(), minOnlineCount, maxOnlineCount, task.staticService(),
                task.autostart(), task.group(), task.proxyGroups());
    }

    private ServiceTask withStatic(ServiceTask task, boolean staticService) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), task.startPort(), task.minOnlineCount(), task.maxOnlineCount(), staticService,
                task.autostart(), task.group(), task.proxyGroups());
    }

    private ServiceTask withAutostart(ServiceTask task, boolean autostart) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), task.startPort(), task.minOnlineCount(), task.maxOnlineCount(), task.staticService(),
                autostart, task.group(), task.proxyGroups());
    }

    private ServiceTask withGroup(ServiceTask task, String group) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), task.startPort(), task.minOnlineCount(), task.maxOnlineCount(), task.staticService(),
                task.autostart(), group, task.proxyGroups());
    }

    private ServiceTask withProxyGroups(ServiceTask task, List<String> proxyGroups) {
        return new ServiceTask(task.name(), task.type(), task.templateName(), task.minMemoryMB(), task.maxMemoryMB(),
                task.cpuCoreLimit(), task.startPort(), task.minOnlineCount(), task.maxOnlineCount(), task.staticService(),
                task.autostart(), task.group(), proxyGroups);
    }

    private void printUsage() {
        CloudLogger.error("Usage: task <create <name> <paper|proxy> <startPort> [template] | list | info <name> | "
                + "edit <name> <field> <value> | delete <name>>");
    }
}
