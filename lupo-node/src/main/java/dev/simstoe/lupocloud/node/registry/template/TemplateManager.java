package dev.simstoe.lupocloud.node.registry.template;

import dev.simstoe.lupocloud.api.models.ServiceType;
import dev.simstoe.lupocloud.node.registry.util.DirectoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class TemplateManager {
    private static final File TEMPLATES_DIR = new File("templates");
    private static final List<String> COPYABLE_ENTRIES = List.of("plugins", "config", "server.properties");
    private static final String MANIFEST_FILE = "template.properties";

    public List<String> listTemplates() {
        File[] folders = TEMPLATES_DIR.listFiles(File::isDirectory);
        if (folders == null) return List.of();
        return Arrays.stream(folders).map(File::getName).sorted().toList();
    }

    public Optional<ServiceType> typeOf(String name) {
        File manifest = new File(new File(TEMPLATES_DIR, name), MANIFEST_FILE);
        if (!manifest.exists()) return Optional.empty();

        var props = new Properties();
        try (var in = new FileInputStream(manifest)) {
            props.load(in);
            return Optional.of(ServiceType.valueOf(props.getProperty("type")));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void saveTemplate(String name, File sourceDir, ServiceType type) throws IOException {
        File templateDir = new File(TEMPLATES_DIR, name);
        DirectoryUtil.deleteRecursively(templateDir.toPath());
        Files.createDirectories(templateDir.toPath());

        for (String entry : COPYABLE_ENTRIES) {
            Path source = new File(sourceDir, entry).toPath();
            if (Files.exists(source)) {
                DirectoryUtil.copyRecursively(source, templateDir.toPath().resolve(entry));
            }
        }

        var manifest = new Properties();
        manifest.setProperty("type", type.name());
        manifest.setProperty("createdAt", Instant.now().toString());
        try (var out = new FileOutputStream(new File(templateDir, MANIFEST_FILE))) {
            manifest.store(out, "Cloud template manifest");
        }
    }

    public void applyTemplate(String name, File targetDir) throws IOException {
        File templateDir = new File(TEMPLATES_DIR, name);
        if (!templateDir.isDirectory()) {
            throw new IOException("Template '" + name + "' existiert nicht.");
        }

        for (String entry : COPYABLE_ENTRIES) {
            Path source = templateDir.toPath().resolve(entry);
            if (Files.exists(source)) {
                DirectoryUtil.copyRecursively(source, targetDir.toPath().resolve(entry));
            }
        }
    }

    public void deleteTemplate(String name) throws IOException {
        File templateDir = new File(TEMPLATES_DIR, name);
        if (!templateDir.isDirectory()) {
            throw new IOException("Template '" + name + "' existiert nicht.");
        }
        DirectoryUtil.deleteRecursively(templateDir.toPath());
    }
}
