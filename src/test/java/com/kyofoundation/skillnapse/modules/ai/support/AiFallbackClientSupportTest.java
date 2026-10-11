package com.kyofoundation.skillnapse.modules.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AiFallbackClientSupportTest {

    @Test
    @DisplayName("Deve reportar indisponibilidade quando nenhuma chave ou provedor de fallback for configurado")
    void deveReportarIndisponivelSemConfiguracao() {
        AiFallbackClientSupport support = new AiFallbackClientSupport();
        ReflectionTestUtils.setField(support, "fallbackEnabled", false);
        ReflectionTestUtils.setField(support, "fallbackApiKey", "");
        ReflectionTestUtils.setField(support, "geminiFallbackApiKey", "");

        assertThat(support.isFallbackDisponivel()).isFalse();
    }

    @Test
    @DisplayName("Deve reportar disponibilidade quando fallbackApiKey (Groq/OpenAI) for configurada")
    void deveReportarDisponivelComChaveOpenAiOuGroq() {
        AiFallbackClientSupport support = new AiFallbackClientSupport();
        ReflectionTestUtils.setField(support, "fallbackApiKey", "gsk_teste123");

        assertThat(support.isFallbackDisponivel()).isTrue();
    }

    @Test
    @DisplayName("Deve reportar disponibilidade quando geminiFallbackApiKey for configurada")
    void deveReportarDisponivelComChaveGeminiSecundaria() {
        AiFallbackClientSupport support = new AiFallbackClientSupport();
        ReflectionTestUtils.setField(support, "geminiFallbackApiKey", "AIzaSy_teste456");

        assertThat(support.isFallbackDisponivel()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar exceção quando tentar executar fallback sem nenhum provedor configurado")
    void deveLancarExcecaoQuandoNenhumProvedorConfigurado() {
        AiFallbackClientSupport support = new AiFallbackClientSupport();
        ReflectionTestUtils.setField(support, "fallbackEnabled", false);
        ReflectionTestUtils.setField(support, "fallbackApiKey", "");
        ReflectionTestUtils.setField(support, "geminiFallbackApiKey", "");

        com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest request =
                new com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest("Teste", null, null, null);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                support.executeFallback(request)
        );
    }
}
