package dev.simstoe.lupocloud.node.registry.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.simstoe.lupocloud.api.models.ServiceTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TaskManager {
    private static final File TASKS_DIR = new File("tasks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public List<String> listTasks() {
        File[] files = TASKS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return List.of();
        return Arrays.stream(files)
                .map(File::getName)
                .map(name -> name.substring(0, name.length() - ".json".length()))
                .sorted()
                .toList();
    }

    public Optional<ServiceTask> getTask(String name) {
        File file = taskFile(name);
        if (!file.isFile()) return Optional.empty();
        try {
            String json = Files.readString(file.toPath());
            return Optional.ofNullable(GSON.fromJson(json, ServiceTask.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void createTask(ServiceTask task) throws IOException {
        File file = taskFile(task.name());
        if (file.exists()) {
            throw new IOException("Task '" + task.name() + "' existiert bereits.");
        }
        writeTask(task);
    }

    public void saveTask(ServiceTask task) throws IOException {
        writeTask(task);
    }

    public void deleteTask(String name) throws IOException {
        File file = taskFile(name);
        if (!file.isFile()) {
            throw new IOException("Task '" + name + "' existiert nicht.");
        }
        Files.delete(file.toPath());
    }

    private void writeTask(ServiceTask task) throws IOException {
        if (!TASKS_DIR.exists()) Files.createDirectories(TASKS_DIR.toPath());
        Files.writeString(taskFile(task.name()).toPath(), GSON.toJson(task));
    }

    private File taskFile(String name) {
        return new File(TASKS_DIR, name.toLowerCase(Locale.ROOT) + ".json");
    }
}
