package dev.simstoe.lupocloud.web.controller;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.PlayerInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final ICloudManager cloudManager;

    public PlayerController(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    @GetMapping
    public List<PlayerInfo> list() {
        return cloudManager.onlinePlayers();
    }
}
