package com.vsign.backend.learning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.learning.dto.AiLandmarkPredictionRequest;
import com.vsign.backend.learning.dto.AiLandmarkPredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AiPredictionProxyService {
    private static final Logger log = LoggerFactory.getLogger(AiPredictionProxyService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String aiServiceBaseUrl;
    private final Duration predictTimeout;

    public AiPredictionProxyService(
            ObjectMapper objectMapper,
            @Value("${ai.service.base-url:http://localhost:8000}") String aiServiceBaseUrl,
            @Value("${ai.service.predict-timeout-ms:10000}") long predictTimeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.aiServiceBaseUrl = aiServiceBaseUrl;
        this.predictTimeout = Duration.ofMillis(predictTimeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public AiLandmarkPredictionResponse predictLandmarks(AiLandmarkPredictionRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest aiRequest = HttpRequest.newBuilder(predictionUri())
                    .timeout(predictTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(aiRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), AiLandmarkPredictionResponse.class);
            }

            String responseBody = response.body() == null ? "" : response.body();
            if (response.statusCode() == 400 || response.statusCode() == 413 || response.statusCode() == 422) {
                log.warn("AI prediction request rejected with status {}: {}", response.statusCode(), truncate(responseBody));
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "AI prediction request rejected");
            }
            log.warn("AI service returned status {}: {}", response.statusCode(), truncate(responseBody));
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI service is unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI service request was interrupted");
        } catch (IOException | IllegalArgumentException exception) {
            log.warn("AI service call failed: {}", exception.toString());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI service is unavailable");
        }
    }

    private URI predictionUri() {
        String normalizedBaseUrl = aiServiceBaseUrl.endsWith("/")
                ? aiServiceBaseUrl.substring(0, aiServiceBaseUrl.length() - 1)
                : aiServiceBaseUrl;
        return URI.create(normalizedBaseUrl + "/predict-landmarks");
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }
}
