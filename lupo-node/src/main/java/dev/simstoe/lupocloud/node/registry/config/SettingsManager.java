package dev.simstoe.lupocloud.node.registry.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.simstoe.lupocloud.api.models.NetworkSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SettingsManager {
    private static final File SETTINGS_FILE = new File("local", "network-settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private volatile NetworkSettings settings = load();

    public NetworkSettings get() {
        return settings;
    }

    public synchronized void update(NetworkSettings newSettings) throws IOException {
        if (!SETTINGS_FILE.getParentFile().exists()) Files.createDirectories(SETTINGS_FILE.getParentFile().toPath());
        Files.writeString(SETTINGS_FILE.toPath(), GSON.toJson(newSettings));
        settings = newSettings;
    }

    private static NetworkSettings load() {
        if (!SETTINGS_FILE.isFile()) return NetworkSettings.defaults();
        try {
            String json = Files.readString(SETTINGS_FILE.toPath());
            NetworkSettings parsed = GSON.fromJson(json, NetworkSettings.class);
            return parsed != null ? parsed : NetworkSettings.defaults();
        } catch (IOException e) {
            return NetworkSettings.defaults();
        }
    }
}
