package dev.simstoe.lupocloud.web.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AdminAccountService {
    private static final File ACCOUNT_FILE = new File("local", "admin-account.json");
    private static final Gson GSON = new GsonBuilder().create();
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;

    private volatile Account account = load();

    public boolean exists() {
        return account != null;
    }

    public synchronized void create(String username, String password) {
        if (account != null) throw new IllegalStateException("Admin account already exists.");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password must not be blank.");
        }

        byte[] salt = randomSalt();
        Account created = new Account(username, Base64.getEncoder().encodeToString(salt), hash(password, salt));
        persist(created);
        this.account = created;
    }

    public boolean matches(String username, String password) {
        Account current = account;
        if (current == null || username == null || password == null) return false;
        if (!current.username.equals(username)) return false;
        byte[] salt = Base64.getDecoder().decode(current.salt);
        return current.hash.equals(hash(password, salt));
    }

    private static Account load() {
        if (!ACCOUNT_FILE.isFile()) return null;
        try {
            String json = Files.readString(ACCOUNT_FILE.toPath());
            return GSON.fromJson(json, Account.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the admin account file.", e);
        }
    }

    private static void persist(Account account) {
        try {
            File parent = ACCOUNT_FILE.getParentFile();
            if (!parent.exists()) Files.createDirectories(parent.toPath());
            Files.writeString(ACCOUNT_FILE.toPath(), GSON.toJson(account));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not persist the admin account.", e);
        }
    }

    private static byte[] randomSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static String hash(String password, byte[] salt) {
        try {
            var spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not hash the password.", e);
        }
    }

    private record Account(String username, String salt, String hash) {}
}
