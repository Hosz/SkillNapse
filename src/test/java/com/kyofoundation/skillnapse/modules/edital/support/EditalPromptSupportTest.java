package com.kyofoundation.skillnapse.modules.edital.support;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EditalPromptSupportTest {

    @Test
    @DisplayName("Deve construir PromptRequest formatando texto do edital e system prompt")
    void deveConstruirPromptRequestCorretamente() {
        EditalPromptSupport support = new EditalPromptSupport();
        String texto = "Conteúdo Programático: Português, Matemática.";

        PromptRequest request = support.criarPromptParaExtracao(texto);

        assertThat(request).isNotNull();
        assertThat(request.prompt()).contains("Português, Matemática.");
        assertThat(request.prompt()).contains("Organize os tópicos e subtópicos em ordem lógica e racional de aprendizado");
        assertThat(request.prompt()).doesNotContain("$s");
        assertThat(request.systemMessage()).contains("Conteúdo Programático");
        assertThat(request.systemMessage()).contains("ORDENAÇÃO LÓGICA E DIDÁTICA DOS TÓPICOS E SUBTÓPICOS");
        assertThat(request.temperature()).isEqualTo(0.2);
        assertThat(request.maxTokens()).isEqualTo(16384);
    }

    @Test
    @DisplayName("Deve construir PromptRequest para detecção de múltiplos cargos")
    void deveConstruirPromptDeteccaoCargosCorretamente() {
        EditalPromptSupport support = new EditalPromptSupport();
        String texto = "Edital de Concurso TRF: Cargos de Analista e Técnico.";

        PromptRequest request = support.criarPromptParaDeteccaoCargos(texto);

        assertThat(request).isNotNull();
        assertThat(request.prompt()).contains("Cargos de Analista e Técnico.");
        assertThat(request.systemMessage()).contains("cargos");
        assertThat(request.temperature()).isEqualTo(0.1);
        assertThat(request.maxTokens()).isEqualTo(8192);
    }
}
