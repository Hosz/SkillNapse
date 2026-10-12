package com.kyofoundation.skillnapse.modules.redacao.support;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.redacao.entity.TemaRedacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromptRedacaoMultimodalSupportTest {

    private final PromptRedacaoMultimodalSupport support = new PromptRedacaoMultimodalSupport();

    @Test
    @DisplayName("Deve construir prompt multimodal com régua de 95% e escala de 0 a 1000 pontos")
    void deveConstruirPromptMultimodalCorretamente() {
        TemaRedacao tema = TemaRedacao.builder()
                .id(UUID.randomUUID())
                .titulo("Impactos da IA no Meio Ambiente")
                .textosMotivadores("Texto motivador 1...")
                .criteriosAvaliacao("Critérios dissertativos...")
                .build();

        PromptRequest promptRequest = support.construirPromptMultimodal(tema);

        assertThat(promptRequest).isNotNull();
        assertThat(promptRequest.prompt()).contains("Impactos da IA no Meio Ambiente");
        assertThat(promptRequest.prompt()).contains("95%");
        assertThat(promptRequest.prompt()).contains("1000.0");
        assertThat(promptRequest.prompt()).contains("textoTranscrito");
        assertThat(promptRequest.systemMessage()).contains("especialista em caligrafia");
    }
}
