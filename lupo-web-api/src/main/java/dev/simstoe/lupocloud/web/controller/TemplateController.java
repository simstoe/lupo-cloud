package dev.simstoe.lupocloud.web.controller;

import dev.simstoe.lupocloud.api.manager.ICloudManager;
import dev.simstoe.lupocloud.api.models.ServiceType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    private final ICloudManager cloudManager;

    public TemplateController(ICloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    public record TemplateDto(String name, ServiceType type) {}

    public record SaveTemplateRequest(String templateName, String sourceService) {}

    @GetMapping
    public List<TemplateDto> list() {
        return Stream.concat(
                cloudManager.templates(ServiceType.PAPER).stream().map(name -> new TemplateDto(name, ServiceType.PAPER)),
                cloudManager.templates(ServiceType.VELOCITY).stream().map(name -> new TemplateDto(name, ServiceType.VELOCITY))
        ).toList();
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody SaveTemplateRequest request) {
        cloudManager.saveTemplate(request.templateName(), request.sourceService());
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        cloudManager.deleteTemplate(name);
        return ResponseEntity.noContent().build();
    }
}
