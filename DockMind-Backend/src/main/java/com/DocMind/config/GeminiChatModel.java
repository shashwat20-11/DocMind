package com.DocMind.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class GeminiChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatModel.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Double temperature;
    private final String apiKey;

    public GeminiChatModel(String apiKey, String model, Double temperature) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null ? model.trim() : "gemini-flash-lite-latest";
        this.temperature = temperature;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        try {
            String jsonPayload = buildPayload(prompt);
            String cleanModel = cleanModelName(this.model);
            String endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + cleanModel + ":generateContent?key=" + this.apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API Error [{}]: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Gemini API error (" + response.statusCode() + "): " + response.body());
            }

            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            String textResponse = extractNativeContent(responseMap);

            return new ChatResponse(List.of(new Generation(new AssistantMessage(textResponse))));

        } catch (Exception e) {
            log.error("Failed executing Gemini unary call: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.<ChatResponse>create(sink -> {
            try {
                String jsonPayload = buildPayload(prompt);
                String cleanModel = cleanModelName(this.model);
                
                // Google Gemini native SSE streaming endpoint
                String endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + cleanModel + ":streamGenerateContent?alt=sse&key=" + this.apiKey;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpointUrl))
                        .header("Content-Type", "application/json")
                        .header("Accept", "text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .timeout(Duration.ofSeconds(120))
                        .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Gemini Stream Error [{}]: {}", response.statusCode(), err);
                    sink.error(new IllegalStateException("Gemini stream error (" + response.statusCode() + "): " + err));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if (!data.isEmpty()) {
                                Map<String, Object> chunkMap = objectMapper.readValue(data, Map.class);
                                String chunkText = extractNativeContent(chunkMap);
                                if (!chunkText.isEmpty()) {
                                    sink.next(new ChatResponse(List.of(new Generation(new AssistantMessage(chunkText)))));
                                }
                            }
                        }
                    }
                }
                sink.complete();

            } catch (Exception e) {
                log.error("Exception during Gemini streaming: {}", e.getMessage(), e);
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }

    private String cleanModelName(String modelName) {
        return modelName.startsWith("models/") ? modelName.substring(7) : modelName;
    }

    private String buildPayload(Prompt prompt) throws Exception {
        var contents = new ArrayList<>();
        Map<String, Object> systemInstruction = null;

        for (Message msg : prompt.getInstructions()) {
            String text = msg.getContent() != null ? msg.getContent() : "";

            if (msg.getMessageType() == MessageType.SYSTEM) {
                systemInstruction = Map.of("parts", List.of(Map.of("text", text)));
            } else {
                String role = (msg.getMessageType() == MessageType.ASSISTANT) ? "model" : "user";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", text))
                ));
            }
        }

        if (contents.isEmpty() && systemInstruction != null) {
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", "Please proceed."))));
        }

        var requestBody = new HashMap<String, Object>();
        requestBody.put("contents", contents);

        if (systemInstruction != null) {
            requestBody.put("systemInstruction", systemInstruction);
        }

        var generationConfig = new HashMap<String, Object>();
        if (this.temperature != null) {
            generationConfig.put("temperature", this.temperature);
        }
        requestBody.put("generationConfig", generationConfig);

        return objectMapper.writeValueAsString(requestBody);
    }

    @SuppressWarnings("unchecked")
    private String extractNativeContent(Map<String, Object> response) {
        if (response == null || !response.containsKey("candidates")) {
            return "";
        }
        List<?> candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidateMap)) {
            return "";
        }
        Object contentObj = candidateMap.get("content");
        if (!(contentObj instanceof Map<?, ?> contentMap)) {
            return "";
        }
        Object partsObj = contentMap.get("parts");
        if (!(partsObj instanceof List<?> partsList) || partsList.isEmpty()) {
            return "";
        }

        // Gemini 2.5 thinking models ke liye: agar multiple parts hain to last part (final answer) pick karein
        StringBuilder combinedText = new StringBuilder();
        for (Object p : partsList) {
            if (p instanceof Map<?, ?> partMap) {
                Object text = partMap.get("text");
                // thought part ko ignore karein agar flag maujood ho
                Boolean isThought = (Boolean) partMap.get("thought");
                if (isThought == null || !isThought) {
                    if (text != null) {
                        combinedText.append(text.toString());
                    }
                }
            }
        }

        if (!combinedText.isEmpty()) {
            return combinedText.toString();
        }

        // Fallback: pehla part extract karein
        Object firstPart = partsList.get(0);
        if (firstPart instanceof Map<?, ?> partMap) {
            Object text = partMap.get("text");
            return text != null ? text.toString() : "";
        }
        return "";
    }
}