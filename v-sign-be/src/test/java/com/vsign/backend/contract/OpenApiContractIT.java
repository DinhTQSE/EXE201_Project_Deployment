package com.vsign.backend.contract;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractIT {

    private static final Path BACKEND_OPENAPI_OUTPUT = Path.of("openapi-backend.json");
    private static final Path FE_OPENAPI_INPUT = Path.of("..", "openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generatesBackendOpenApiAndCoversFeFirstPaths() throws Exception {
        String rawSpec = mockMvc.perform(get("/api/v1/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode backendSpec = objectMapper.readTree(rawSpec);
        String prettySpec = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(backendSpec)
                + System.lineSeparator();
        Files.writeString(BACKEND_OPENAPI_OUTPUT, prettySpec, StandardCharsets.UTF_8);

        assertTrue(backendSpec.path("paths").has("/api/v1/auth/register"));
        assertTrue(backendSpec.path("paths").has("/api/v1/dictionary"));
        assertTrue(backendSpec.path("paths").has("/api/v1/subscription/plans"));
        assertTrue(backendSpec.path("paths").has("/api/v1/payments/orders"));

        if (Files.exists(FE_OPENAPI_INPUT)) {
            assertFeFirstPathsAreCovered(backendSpec.path("paths"));
        }
    }

    private void assertFeFirstPathsAreCovered(JsonNode backendPaths) throws Exception {
        JsonNode fePaths = objectMapper.readTree(Files.readString(FE_OPENAPI_INPUT, StandardCharsets.UTF_8)).path("paths");
        Iterator<String> pathNames = fePaths.fieldNames();
        while (pathNames.hasNext()) {
            String fePath = pathNames.next();
            String backendPath = "/api/v1" + fePath;
            assertTrue(
                    backendPaths.has(backendPath),
                    () -> "Missing backend OpenAPI path for FE contract: " + backendPath
            );
        }
    }
}
