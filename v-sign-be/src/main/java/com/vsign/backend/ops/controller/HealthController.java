package com.vsign.backend.ops.controller;

import com.vsign.backend.common.response.SuccessResponse;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private final String applicationName;
    private final BuildProperties buildProperties;

    public HealthController(
            @Value("${spring.application.name:v-sign-backend}") String applicationName,
            ObjectProvider<BuildProperties> buildProperties
    ) {
        this.applicationName = applicationName;
        this.buildProperties = buildProperties.getIfAvailable();
    }

    @GetMapping("/health")
    public SuccessResponse<HealthResponse> health() {
        return SuccessResponse.ok("Backend healthy", new HealthResponse("UP", applicationName, OffsetDateTime.now()));
    }

    @GetMapping("/version")
    public SuccessResponse<VersionResponse> version() {
        String version = buildProperties == null ? "dev" : buildProperties.getVersion();
        return SuccessResponse.ok("Backend version loaded", new VersionResponse(applicationName, version));
    }

    public record HealthResponse(String status, String service, OffsetDateTime checkedAt) {
    }

    public record VersionResponse(String service, String version) {
    }
}
