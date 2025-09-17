package com.example.policyassistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BedrockChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockChatService.class);

    private final BedrockRuntimeClient client;
    private final ObjectMapper objectMapper;
    private final String modelId;

    public BedrockChatService(BedrockRuntimeClient client,
                              ObjectMapper objectMapper,
                              @Value("${bedrock.model-id}") String modelId) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.modelId = modelId;
    }

    public String askModel(String systemPrompt, String userPrompt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "gpt-4o-mini");
        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", List.of(Map.of("type", "text", "text", systemPrompt)));
            messages.add(systemMessage);
        }
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", List.of(Map.of("type", "text", "text", userPrompt)));
        messages.add(userMessage);
        payload.put("messages", messages);
        payload.put("temperature", 0);
        payload.put("max_output_tokens", 800);
        try {
            String body = objectMapper.writeValueAsString(payload);
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromString(body, StandardCharsets.UTF_8))
                    .build();
            InvokeModelResponse response = client.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            return extractText(responseBody);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize request for Bedrock", e);
            return "I don't know.";
        } catch (Exception e) {
            LOGGER.error("Bedrock invocation failed", e);
            return "I don't know.";
        }
    }

    private String extractText(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "I don't know.";
        }
        JsonNode message = choices.get(0).path("message");
        JsonNode content = message.path("content");
        if (content.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode node : content) {
                if (node.has("text")) {
                    builder.append(node.get("text").asText());
                }
            }
            String result = builder.toString().trim();
            return result.isEmpty() ? "I don't know." : result;
        }
        if (message.has("content")) {
            String text = message.get("content").asText().trim();
            return text.isEmpty() ? "I don't know." : text;
        }
        return "I don't know.";
    }
}
