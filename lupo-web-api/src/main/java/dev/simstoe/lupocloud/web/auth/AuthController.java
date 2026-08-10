package dev.simstoe.lupocloud.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminAccountService adminAccount;
    private final TokenStore tokenStore;

    public AuthController(AdminAccountService adminAccount, TokenStore tokenStore) {
        this.adminAccount = adminAccount;
        this.tokenStore = tokenStore;
    }

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String token) {}

    public record CreateAdminRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!adminAccount.matches(request.username(), request.password())) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new LoginResponse(tokenStore.issue()));
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<LoginResponse> bootstrap() {
        if (adminAccount.exists()) {
            return ResponseEntity.status(409).build();
        }
        return ResponseEntity.ok(new LoginResponse(tokenStore.issue()));
    }

    @PostMapping("/create-admin")
    public ResponseEntity<Void> createAdmin(@RequestBody CreateAdminRequest request) {
        if (adminAccount.exists()) {
            return ResponseEntity.status(409).build();
        }
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        adminAccount.create(request.username(), request.password());
        return ResponseEntity.noContent().build();
    }
}
