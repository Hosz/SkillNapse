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
        assertThat(request.prompt()).doesNotContain("$s");
        assertThat(request.systemMessage()).contains("Conteúdo Programático");
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
    @DisplayName("Deve extrair trecho compacto para detecção de cargos quando o edital for extenso")
    void deveOtimizarTrechoParaDeteccaoCargosEmEditalExtenso() {
        String textoLongo = "Preâmbulo do Edital Concurso TJPE " + "A".repeat(30_000)
                + "\nQuadro de Vagas e Cargos:\n1. Analista Judiciário\n2. Oficial de Justiça\n"
                + "B".repeat(10_000);

        String resultado = EditalPromptSupport.extrairTrechoParaDeteccaoCargos(textoLongo);

        assertThat(resultado).isNotNull();
        assertThat(resultado.length()).isLessThan(textoLongo.length());
        assertThat(resultado).contains("Preâmbulo do Edital Concurso TJPE");
        assertThat(resultado).contains("SEÇÃO DE VAGAS/CARGOS IDENTIFICADA NO EDITAL");
        assertThat(resultado).contains("Analista Judiciário");
    }

    @Test
    @DisplayName("Deve focar no trecho de conteúdo programático quando o edital for extenso")
    void deveOtimizarTrechoParaConteudoProgramaticoEmEditalExtenso() {
        String textoLongo = "Preâmbulo e regras do edital " + "X".repeat(90_000)
                + "\nConteúdo Programático:\nLíngua Portuguesa: Ortografia, Sintaxe.\n"
                + "Y".repeat(10_000);

        String resultado = EditalPromptSupport.extrairTrechoParaConteudoProgramatico(textoLongo, "Soldado");

        assertThat(resultado).isNotNull();
        assertThat(resultado.length()).isLessThan(textoLongo.length());
        assertThat(resultado).contains("Preâmbulo e regras do edital");
        assertThat(resultado).contains("INÍCIO DO CONTEÚDO PROGRAMÁTICO IDENTIFICADO");
        assertThat(resultado).contains("Língua Portuguesa: Ortografia, Sintaxe.");
    }
}
