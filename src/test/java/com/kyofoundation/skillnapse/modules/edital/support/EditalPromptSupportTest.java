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
        assertThat(request.prompt()).contains("ORDENAÇÃO POR GRAFO DE DEPENDÊNCIAS CONCEITUAIS");
        assertThat(request.prompt()).contains("AGRUPAMENTO POR AFINIDADE ONTOLÓGICA");
        assertThat(request.prompt()).contains("PRESERVAÇÃO DE FIDELIDADE (ZERO INTERPOLAÇÃO)");
        assertThat(request.prompt()).doesNotContain("$s");
        assertThat(request.systemMessage()).contains("ISOLAMENTO ESTRITO DE ESCOPO (BOUNDARY ENFORCEMENT)");
        assertThat(request.systemMessage()).contains("ORDENAÇÃO POR GRAFO DE DEPENDÊNCIAS CONCEITUAIS (PEDAGOGICAL ORDERING)");
        assertThat(request.systemMessage()).contains("AGRUPAMENTO POR AFINIDADE ONTOLÓGICA (COERÊNCIA TAXONÔMICA)");
        assertThat(request.systemMessage()).contains("PRESERVAÇÃO DE FIDELIDADE (ZERO INTERPOLAÇÃO)");
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

    @Test
    @DisplayName("Deve construir PromptRequest aplicando isolamento estrito de escopo quando cargoAlvo for informado")
    void deveConstruirPromptComIsolamentoEscopoParaCargoAlvo() {
        EditalPromptSupport support = new EditalPromptSupport();
        String texto = "Conteúdo Programático com múltiplos cargos.";
        String cargo = "Soldado PM";

        PromptRequest request = support.criarPromptParaExtracao(texto, cargo);

        assertThat(request).isNotNull();
        assertThat(request.prompt()).contains("ISOLAMENTO ESTRITO DE ESCOPO (BOUNDARY ENFORCEMENT)");
        assertThat(request.prompt()).contains("Perfil/Cargo Alvo Selecionado: \"Soldado PM\"");
        assertThat(request.prompt()).contains("Isole cirurgicamente a secção documental");
    }
}
