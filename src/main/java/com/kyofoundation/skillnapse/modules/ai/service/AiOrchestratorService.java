package com.kyofoundation.skillnapse.modules.ai.service;

import com.kyofoundation.skillnapse.common.exception.AiQuotaExceededException;
import com.kyofoundation.skillnapse.modules.ai.dto.AiGenerationResponse;
import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.validator.AiPromptValidator;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiOrchestratorService {

    private final ChatClient.Builder chatClientBuilder;
    private final ChatModel chatModel;
    private final AiPromptValidator aiPromptValidator;
    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final com.kyofoundation.skillnapse.modules.ai.support.AiFallbackClientSupport aiFallbackClientSupport;

    @Value("${skillnapse.ai.provider:}")
    private String configuredProvider;

    public AiGenerationResponse generate(UUID userId, PromptRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);
        return generate(request);
    }

    public AiGenerationResponse generate(PromptRequest request) {
        aiPromptValidator.validatePromptRequest(request);

        return executeWithQuotaHandling(
                () -> {
                    ChatClient chatClient = chatClientBuilder.build();
                    ChatClient.ChatClientRequestSpec spec = chatClient.prompt().user(request.prompt());
                    spec = applyPromptOptions(spec, request);

                    ChatResponse chatResponse = spec.call().chatResponse();

                    String content = "";
                    String model = "unknown";
                    Long promptTokens = 0L;
                    Long generationTokens = 0L;
                    Long totalTokens = 0L;

                    if (chatResponse != null) {
                        if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                            content = chatResponse.getResult().getOutput().getText();
                        }
                        if (chatResponse.getMetadata() != null) {
                            if (chatResponse.getMetadata().getModel() != null) {
                                model = chatResponse.getMetadata().getModel();
                            }
                            Usage usage = chatResponse.getMetadata().getUsage();
                            if (usage != null) {
                                promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens().longValue() : 0L;
                                generationTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens().longValue() : 0L;
                                totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens().longValue() : (promptTokens + generationTokens);
                            }
                        }
                    }

                    return new AiGenerationResponse(
                            content != null ? content : "",
                            getActiveProviderName(),
                            model,
                            promptTokens,
                            generationTokens,
                            totalTokens,
                            Instant.now()
                    );
                },
                () -> aiFallbackClientSupport.executeFallback(request),
                "generate"
        );
    }

    public <T> T generateStructured(PromptRequest request, Class<T> responseType) {
        aiPromptValidator.validateStructuredRequest(request, responseType);

        BeanOutputConverter<T> converter = new BeanOutputConverter<>(
                responseType,
                null,
                AiOrchestratorService::cleanJsonText
        );

        return executeWithQuotaHandling(
                () -> {
                    ChatClient chatClient = chatClientBuilder.build();
                    ChatClient.ChatClientRequestSpec spec = chatClient.prompt().user(request.prompt());
                    spec = applyStructuredOptions(spec, request);

                    return spec.call().entity(converter);
                },
                () -> aiFallbackClientSupport.executeFallbackStructured(request, responseType),
                "generateStructured (" + responseType.getSimpleName() + ")"
        );
    }

    public <T> T generateStructuredMultimodal(
            PromptRequest request,
            org.springframework.util.MimeType mimeType,
            org.springframework.core.io.Resource mediaResource,
            Class<T> responseType
    ) {
        aiPromptValidator.validateStructuredRequest(request, responseType);

        BeanOutputConverter<T> converter = new BeanOutputConverter<>(
                responseType,
                null,
                AiOrchestratorService::cleanJsonText
        );

        return executeWithQuotaHandling(
                () -> {
                    ChatClient chatClient = chatClientBuilder.build();
                    ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                            .user(userSpec -> userSpec.text(request.prompt()).media(mimeType, mediaResource));
                    spec = applyStructuredOptions(spec, request);

                    return spec.call().entity(converter);
                },
                () -> aiFallbackClientSupport.executeFallbackStructured(request, responseType),
                "generateStructuredMultimodal (" + responseType.getSimpleName() + ")"
        );
    }

    @FunctionalInterface
    private interface SupplierWithException<R> {
        R get() throws Exception;
    }

    private <R> R executeWithQuotaHandling(
            SupplierWithException<R> primaryAction,
            SupplierWithException<R> fallbackAction,
            String operationName
    ) {
        boolean fallbackArmado = aiFallbackClientSupport != null && aiFallbackClientSupport.isFallbackDisponivel();

        try {
            return primaryAction.get();
        } catch (Exception ex) {
            log.warn("Provedor principal [{}] falhou em '{}' ({}). Verificando contingência...",
                    getActiveProviderName(), operationName, ex.getMessage());

            if (isRateLimitError(ex)) {
                log.warn("Taxa limite temporária (429) detectada em {}. Tentando retry com backoff de 2s...", operationName);
                try {
                    Thread.sleep(2000);
                    return primaryAction.get();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Exception retryEx) {
                    log.warn("Retry no provedor principal também falhou: {}", retryEx.getMessage());
                    ex = retryEx;
                }
            }

            if (fallbackArmado) {
                try {
                    log.info("Acionando provedor secundário de contingência para {}...", operationName);
                    return fallbackAction.get();
                } catch (Exception fallbackEx) {
                    log.error("Falha também no provedor secundário de IA: {}", fallbackEx.getMessage());
                }
            }

            if (isQuotaOrRateLimitException(ex)) {
                log.warn("Cota de IA definitivamente esgotada em {}: {}", operationName, ex.getMessage());
                throw new AiQuotaExceededException(
                        "A cota diária de requisições de Inteligência Artificial foi atingida no provedor (429). A renovação ocorre automaticamente à meia-noite (UTC).",
                        ex
                );
            }

            if (ex instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(ex);
        }
    }

    public static boolean isRateLimitError(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("429")
                    || lower.contains("rate-limit")
                    || lower.contains("rate limit")
                    || lower.contains("too many requests")
                    || lower.contains("resource_exhausted")) {
                return true;
            }
        }
        return isRateLimitError(t.getCause());
    }

    public static boolean isQuotaOrRateLimitException(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("429")
                    || lower.contains("quota")
                    || lower.contains("rate-limit")
                    || lower.contains("rate limit")
                    || lower.contains("resource_exhausted")
                    || lower.contains("exhausted")
                    || lower.contains("too many requests")
                    || lower.contains("exceeded your current quota")
                    || lower.contains("api_key_invalid")
                    || lower.contains("api key not valid")
                    || lower.contains("invalid api key")
                    || lower.contains("invalid authentication credentials")
                    || lower.contains("access_token_type_unsupported")
                    || lower.contains("401")
                    || lower.contains("unauthenticated")
                    || lower.contains("permission_denied")) {
                return true;
            }
        }
        return isQuotaOrRateLimitException(t.getCause());
    }

    public String getActiveProviderName() {
        if (configuredProvider != null && !configuredProvider.isBlank()) {
            return configuredProvider;
        }
        if (chatModel != null) {
            String className = chatModel.getClass().getSimpleName().toLowerCase();
            if (className.contains("google") || className.contains("gemini")) {
                return "gemini";
            }
            if (className.contains("openai")) {
                return "openai";
            }
            if (className.contains("ollama")) {
                return "ollama";
            }
        }
        return "gemini";
    }

    public static String cleanJsonText(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String text = raw.trim();

        // 1. Remove tags de pensamento/raciocínio (<think>...</think>)
        if (text.contains("<think>") && text.contains("</think>")) {
            int endThink = text.lastIndexOf("</think>");
            text = text.substring(endThink + "</think>".length()).trim();
        }

        // 2. Extrai bloco de código Markdown se presente (```json ... ``` ou ``` ... ```)
        int codeBlockStart = text.indexOf("```json");
        if (codeBlockStart == -1) {
            codeBlockStart = text.indexOf("```");
        }

        if (codeBlockStart != -1) {
            int afterFence = codeBlockStart + (text.startsWith("```json", codeBlockStart) ? 7 : 3);
            int codeBlockEnd = text.lastIndexOf("```");
            if (codeBlockEnd > afterFence) {
                text = text.substring(afterFence, codeBlockEnd).trim();
            } else {
                text = text.substring(afterFence).trim();
            }
        }

        // 3. Extrai estritamente o objeto ({ ... }) ou array ([ ... ]) mais externo
        int firstBrace = text.indexOf('{');
        int firstBracket = text.indexOf('[');

        int start = -1;
        int end = -1;

        if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
            start = firstBrace;
            end = text.lastIndexOf('}');
        } else if (firstBracket != -1) {
            start = firstBracket;
            end = text.lastIndexOf(']');
        }

        if (start != -1 && end != -1 && end > start) {
            text = text.substring(start, end + 1).trim();
        } else if (start != -1) {
            text = text.substring(start).trim();
        }

        // 4. Auto-reparo de JSON truncado por limite de tokens ou corte abrupto
        return repairTruncatedJson(text);
    }

    public static String repairTruncatedJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        int firstBrace = json.indexOf('{');
        int firstBracket = json.indexOf('[');
        int start = -1;
        if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
            start = firstBrace;
        } else if (firstBracket != -1) {
            start = firstBracket;
        }

        if (start == -1) {
            return json;
        }

        String content = json.substring(start).trim();

        boolean inString = false;
        boolean escape = false;
        java.util.ArrayDeque<Character> stack = new java.util.ArrayDeque<>();

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                if (inString) {
                    escape = true;
                }
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{' || c == '[') {
                    stack.push(c);
                } else if (c == '}' || c == ']') {
                    if (!stack.isEmpty()) {
                        char open = stack.peek();
                        if ((c == '}' && open == '{') || (c == ']' && open == '[')) {
                            stack.pop();
                        }
                    }
                }
            }
        }

        if (!inString && stack.isEmpty()) {
            return content;
        }

        StringBuilder repaired = new StringBuilder(content);

        if (inString) {
            if (repaired.length() > 0 && repaired.charAt(repaired.length() - 1) == '\\') {
                repaired.deleteCharAt(repaired.length() - 1);
            }
            repaired.append('"');
        }

        while (repaired.length() > 0 && Character.isWhitespace(repaired.charAt(repaired.length() - 1))) {
            repaired.deleteCharAt(repaired.length() - 1);
        }

        if (repaired.length() > 0 && repaired.charAt(repaired.length() - 1) == ',') {
            repaired.deleteCharAt(repaired.length() - 1);
        } else if (repaired.length() > 0 && repaired.charAt(repaired.length() - 1) == ':') {
            repaired.append("null");
        }

        while (!stack.isEmpty()) {
            char open = stack.pop();
            int lastIdx = repaired.length() - 1;
            while (lastIdx >= 0 && Character.isWhitespace(repaired.charAt(lastIdx))) {
                lastIdx--;
            }
            if (lastIdx >= 0 && repaired.charAt(lastIdx) == ',') {
                repaired.deleteCharAt(lastIdx);
            }
            if (open == '{') {
                repaired.append('}');
            } else if (open == '[') {
                repaired.append(']');
            }
        }

        return repaired.toString().trim();
    }

    private ChatClient.ChatClientRequestSpec applyStructuredOptions(ChatClient.ChatClientRequestSpec spec, PromptRequest request) {
        if (request.systemMessage() != null && !request.systemMessage().isBlank()) {
            spec = spec.system(request.systemMessage());
        }

        String provider = getActiveProviderName();
        if ("gemini".equalsIgnoreCase(provider)) {
            try {
                Class<?> optionsClass = Class.forName("org.springframework.ai.google.genai.GoogleGenAiChatOptions");
                Object builder = optionsClass.getMethod("builder").invoke(null);
                builder.getClass().getMethod("responseMimeType", String.class).invoke(builder, "application/json");

                if (request.temperature() != null) {
                    builder.getClass().getMethod("temperature", Double.class).invoke(builder, request.temperature());
                }
                if (request.maxTokens() != null) {
                    builder.getClass().getMethod("maxOutputTokens", Integer.class).invoke(builder, request.maxTokens());
                }

                if (builder instanceof ChatOptions.Builder<?> chatOptionsBuilder) {
                    return spec.options(chatOptionsBuilder);
                }
            } catch (Throwable t) {
                log.debug("Não foi possível configurar GoogleGenAiChatOptions via reflexão: {}", t.getMessage());
            }
        }

        return applyPromptOptions(spec, request);
    }

    private ChatClient.ChatClientRequestSpec applyPromptOptions(ChatClient.ChatClientRequestSpec spec, PromptRequest request) {
        if (request.systemMessage() != null && !request.systemMessage().isBlank()) {
            spec = spec.system(request.systemMessage());
        }

        ChatOptions.Builder<?> optionsBuilder = ChatOptions.builder();
        boolean hasOptions = false;

        if (request.temperature() != null) {
            optionsBuilder = optionsBuilder.temperature(request.temperature());
            hasOptions = true;
        }

        if (request.maxTokens() != null) {
            optionsBuilder = optionsBuilder.maxTokens(request.maxTokens());
            hasOptions = true;
        }

        if (hasOptions) {
            spec = spec.options(optionsBuilder);
        }

        return spec;
    }
}
