package com.kyofoundation.skillnapse.modules.ai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyofoundation.skillnapse.modules.ai.dto.AiGenerationResponse;
import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiFallbackClientSupport {

    private final ObjectMapper objectMapper = createConfiguredObjectMapper();
    private final RestClient restClient = RestClient.builder().build();

    private static ObjectMapper createConfiguredObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        return mapper;
    }

    @Value("${skillnapse.ai.fallback.enabled:false}")
    private boolean fallbackEnabled;

    @Value("${skillnapse.ai.fallback.provider:groq}")
    private String fallbackProvider;

    @Value("${skillnapse.ai.fallback.base-url:https://api.groq.com/openai/v1}")
    private String fallbackBaseUrl;

    @Value("${skillnapse.ai.fallback.api-key:}")
    private String fallbackApiKey;

    @Value("${skillnapse.ai.fallback.model:openai/gpt-oss-120b}")
    private String fallbackModel;

    @Value("${skillnapse.ai.fallback.gemini-api-key:}")
    private String geminiFallbackApiKey;

    @Value("${skillnapse.ai.fallback.gemini-model:gemini-3.8-flash}")
    private String geminiFallbackModel;

    @PostConstruct
    public void logConfiguracao() {
        if (!fallbackEnabled) {
            log.info("[IA Fallback] Contingência multi-provedor desativada (skillnapse.ai.fallback.enabled=false).");
            return;
        }
        boolean temGemini = geminiFallbackApiKey != null && !geminiFallbackApiKey.isBlank();
        boolean temOpenAi = fallbackApiKey != null && !fallbackApiKey.isBlank();
        log.info("[IA Fallback] Sistema de contingência ATIVO! Preferência: [{}]. Provedor OpenAI/Groq={}[{}], Gemini Secundário={}[{}].",
                fallbackProvider,
                temOpenAi ? "CONFIGURADO" : "NÃO CONFIGURADO",
                fallbackModel,
                temGemini ? "CONFIGURADO" : "NÃO CONFIGURADO",
                geminiFallbackModel
        );
    }

    public boolean isFallbackDisponivel() {
        if (geminiFallbackApiKey != null && !geminiFallbackApiKey.isBlank()) {
            return true;
        }
        if (fallbackApiKey != null && !fallbackApiKey.isBlank()) {
            return true;
        }
        return "ollama".equalsIgnoreCase(fallbackProvider) && fallbackEnabled;
    }

    public AiGenerationResponse executeFallback(PromptRequest request) {
        log.info("Acionando cadeia de provedores de IA secundários (fallback)...");

        boolean temGemini = geminiFallbackApiKey != null && !geminiFallbackApiKey.isBlank();
        boolean temOpenAiCompativel = (fallbackApiKey != null && !fallbackApiKey.isBlank())
                || ("ollama".equalsIgnoreCase(fallbackProvider) && fallbackEnabled);

        boolean priorizarGemini = "gemini".equalsIgnoreCase(fallbackProvider);

        List<FallbackStep> chain = new ArrayList<>();
        if (priorizarGemini) {
            if (temGemini) {
                chain.add(new FallbackStep("Gemini Secundário", () -> chamarGeminiSecundario(request)));
            }
            if (temOpenAiCompativel) {
                String nome = fallbackProvider != null && !fallbackProvider.isBlank() ? fallbackProvider : "OpenAI-Compatível";
                chain.add(new FallbackStep(nome, () -> chamarOpenAiCompativel(request)));
            }
        } else {
            if (temOpenAiCompativel) {
                String nome = fallbackProvider != null && !fallbackProvider.isBlank() ? fallbackProvider : "OpenAI-Compatível";
                chain.add(new FallbackStep(nome, () -> chamarOpenAiCompativel(request)));
            }
            if (temGemini) {
                chain.add(new FallbackStep("Gemini Secundário", () -> chamarGeminiSecundario(request)));
            }
        }

        if (chain.isEmpty()) {
            throw new IllegalStateException("Nenhum provedor de fallback configurado.");
        }

        Exception ultimoErro = null;
        for (FallbackStep step : chain) {
            try {
                log.info("Tentando contingência via provedor de fallback: {}", step.name());
                AiGenerationResponse response = step.action().call();
                log.info("[IA Fallback] Resposta obtida com sucesso via [{}]!", step.name());
                return response;
            } catch (Exception ex) {
                if (AiOrchestratorService.isTransientError(ex)) {
                    log.warn("Instabilidade temporária detectada no fallback [{}] ({}). Tentando retry com backoff de 1.5s...", step.name(), ex.getMessage());
                    try {
                        Thread.sleep(1500);
                        AiGenerationResponse retryResponse = step.action().call();
                        log.info("[IA Fallback] Resposta obtida com sucesso via [{}] após retry!", step.name());
                        return retryResponse;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception retryEx) {
                        log.warn("Retry no fallback [{}] também falhou: {}. Tentando próximo elo da cadeia...", step.name(), retryEx.getMessage());
                        ultimoErro = retryEx;
                        continue;
                    }
                }
                log.warn("Fallback via [{}] falhou: {}. Tentando próximo elo da cadeia...", step.name(), ex.getMessage());
                ultimoErro = ex;
            }
        }

        if (ultimoErro instanceof RuntimeException re) {
            throw re;
        }
        throw new RuntimeException("Todos os provedores de fallback configurados falharam.", ultimoErro);
    }

    @FunctionalInterface
    private interface FallbackAction {
        AiGenerationResponse call() throws Exception;
    }

    private record FallbackStep(String name, FallbackAction action) {}

    public <T> T executeFallbackStructured(PromptRequest request, Class<T> responseType) {
        log.info("Acionando provedor de IA secundário (fallback estruturado para {})...", responseType.getSimpleName());

        AiGenerationResponse response = executeFallback(request);
        String jsonLimpo = AiOrchestratorService.cleanJsonText(response.content());

        try {
            return objectMapper.readValue(jsonLimpo, responseType);
        } catch (Exception ex) {
            log.error("Falha ao desserializar resposta JSON do fallback para {}: {}", responseType.getSimpleName(), ex.getMessage());
            throw new RuntimeException("Falha na interpretação da resposta JSON do provedor de fallback", ex);
        }
    }

    private AiGenerationResponse chamarOpenAiCompativel(PromptRequest request) {
        RestClient client = this.restClient;
        String url = normalizarUrlCompletions(fallbackBaseUrl);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", fallbackModel);
        if (request.temperature() != null) {
            payload.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            payload.put("max_tokens", request.maxTokens());
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (request.systemMessage() != null && !request.systemMessage().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.systemMessage()));
        }
        messages.add(Map.of("role", "user", "content", request.prompt()));
        payload.put("messages", messages);

        RestClient.RequestBodySpec requestSpec = client.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON);

        if (fallbackApiKey != null && !fallbackApiKey.isBlank()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + fallbackApiKey.trim());
        }

        String rawResponse;
        try {
            rawResponse = requestSpec
                    .body(payload)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if ((msg.contains("413") || msg.contains("request too large") || msg.contains("rate_limit_exceeded"))
                    && "openai/gpt-oss-120b".equals(payload.get("model"))) {
                log.warn("Modelo openai/gpt-oss-120b excedeu limite de tokens na Groq. Tentando automaticamente openai/gpt-oss-20b...");
                payload.put("model", "openai/gpt-oss-20b");
                RestClient.RequestBodySpec retrySpec = client.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON);
                if (fallbackApiKey != null && !fallbackApiKey.isBlank()) {
                    retrySpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + fallbackApiKey.trim());
                }
                rawResponse = retrySpec
                        .body(payload)
                        .retrieve()
                        .body(String.class);
            } else {
                throw ex;
            }
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            String model = root.path("model").asText(fallbackModel);

            JsonNode usage = root.path("usage");
            long promptTokens = usage.path("prompt_tokens").asLong(0L);
            long completionTokens = usage.path("completion_tokens").asLong(0L);
            long totalTokens = usage.path("total_tokens").asLong(promptTokens + completionTokens);

            return new AiGenerationResponse(
                    content,
                    fallbackProvider != null ? fallbackProvider : "openai-compatible",
                    model,
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    Instant.now()
            );
        } catch (Exception ex) {
            log.error("Erro ao analisar resposta da API OpenAI-compatible: {}", ex.getMessage());
            throw new RuntimeException("Erro ao processar resposta do provedor de fallback", ex);
        }
    }

    private AiGenerationResponse chamarGeminiSecundario(PromptRequest request) {
        RestClient client = this.restClient;
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiFallbackModel,
                geminiFallbackApiKey.trim()
        );

        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(Map.of("parts", List.of(Map.of("text", request.prompt()))));
        payload.put("contents", contents);

        if (request.systemMessage() != null && !request.systemMessage().isBlank()) {
            payload.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.systemMessage()))));
        }

        Map<String, Object> genConfig = new HashMap<>();
        if (request.temperature() != null) {
            genConfig.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            genConfig.put("maxOutputTokens", request.maxTokens());
        }
        if (!genConfig.isEmpty()) {
            payload.put("generationConfig", genConfig);
        }

        String rawResponse = client.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");

            JsonNode usage = root.path("usageMetadata");
            long promptTokens = usage.path("promptTokenCount").asLong(0L);
            long completionTokens = usage.path("candidatesTokenCount").asLong(0L);
            long totalTokens = usage.path("totalTokenCount").asLong(promptTokens + completionTokens);

            return new AiGenerationResponse(
                    content,
                    "gemini-fallback",
                    geminiFallbackModel,
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    Instant.now()
            );
        } catch (Exception ex) {
            log.error("Erro ao analisar resposta da API secundária do Gemini: {}", ex.getMessage());
            throw new RuntimeException("Erro ao processar resposta do Gemini secundário", ex);
        }
    }

    private String normalizarUrlCompletions(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.groq.com/openai/v1/chat/completions";
        }
        String url = baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.endsWith("/chat/completions")) {
            url = url + "/chat/completions";
        }
        return url;
    }
}
