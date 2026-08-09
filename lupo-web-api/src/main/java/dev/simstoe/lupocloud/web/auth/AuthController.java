package dev.simstoe.lupocloud.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final WebAdminSecret adminSecret;
    private final TokenStore tokenStore;

    public AuthController(WebAdminSecret adminSecret, TokenStore tokenStore) {
        this.adminSecret = adminSecret;
        this.tokenStore = tokenStore;
    }

    public record LoginRequest(String password) {}

    public record LoginResponse(String token) {}

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.password() == null || !adminSecret.matches(request.password())) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new LoginResponse(tokenStore.issue()));
    }
}
