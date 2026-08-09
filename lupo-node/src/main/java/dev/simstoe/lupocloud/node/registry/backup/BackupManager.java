package dev.simstoe.lupocloud.node.registry.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    private static final File BACKUPS_DIR = new File("backups");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public File createBackup(String serviceName, File sourceDir) throws IOException {
        File serviceBackupDir = serviceBackupDir(serviceName);
        if (!serviceBackupDir.exists()) Files.createDirectories(serviceBackupDir.toPath());

        File zipFile = new File(serviceBackupDir, TIMESTAMP_FORMAT.format(LocalDateTime.now()) + ".zip");
        Path sourcePath = sourceDir.toPath();

        try (var zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            try (var files = Files.walk(sourcePath)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        String entryName = sourcePath.relativize(file).toString().replace(File.separatorChar, '/');
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } catch (UncheckedIOException e) {
            zipFile.delete();
            throw e.getCause();
        } catch (IOException e) {
            zipFile.delete();
            throw e;
        }

        return zipFile;
    }

    public List<String> listBackups(String serviceName) {
        File[] files = serviceBackupDir(serviceName).listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) return List.of();
        return Arrays.stream(files).map(File::getName).sorted().toList();
    }

    public void restoreBackup(String serviceName, String backupFileName, File targetDir) throws IOException {
        File zipFile = new File(serviceBackupDir(serviceName), backupFileName);
        if (!zipFile.isFile()) {
            throw new IOException("Backup '" + backupFileName + "' for '" + serviceName + "' does not exist.");
        }

        if (!targetDir.exists()) Files.createDirectories(targetDir.toPath());
        String targetCanonicalPath = targetDir.getCanonicalPath();

        try (var zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());
                String outCanonicalPath = outFile.getCanonicalPath();
                if (!outCanonicalPath.equals(targetCanonicalPath) && !outCanonicalPath.startsWith(targetCanonicalPath + File.separator)) {
                    throw new IOException("Backup entry '" + entry.getName() + "' would escape the target directory - aborting restore.");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outFile.toPath());
                } else {
                    Files.createDirectories(outFile.getParentFile().toPath());
                    Files.copy(zis, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    public void deleteBackup(String serviceName, String backupFileName) throws IOException {
        File zipFile = new File(serviceBackupDir(serviceName), backupFileName);
        if (!zipFile.isFile()) {
            throw new IOException("Backup '" + backupFileName + "' for '" + serviceName + "' does not exist.");
        }
        Files.delete(zipFile.toPath());
    }

    private File serviceBackupDir(String serviceName) {
        return new File(BACKUPS_DIR, serviceName.toLowerCase(Locale.ROOT));
    }
}
