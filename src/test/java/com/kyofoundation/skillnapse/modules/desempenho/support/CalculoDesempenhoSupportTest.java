package com.kyofoundation.skillnapse.modules.desempenho.support;

import com.kyofoundation.skillnapse.modules.desempenho.dto.response.TopicoLacunaResponse;
import com.kyofoundation.skillnapse.modules.desempenho.enums.NivelCriticidadeTopico;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoDesempenhoSupportTest {

    private CalculoDesempenhoSupport support;

    @BeforeEach
    void setUp() {
        support = new CalculoDesempenhoSupport();
    }

    @Test
    @DisplayName("Deve calcular taxa de acerto com precisao de uma casa decimal")
    void deveCalcularTaxaAcerto() {
        assertThat(support.calcularTaxaAcerto(10L, 8L)).isEqualTo(80.0);
        assertThat(support.calcularTaxaAcerto(3L, 1L)).isEqualTo(33.3);
        assertThat(support.calcularTaxaAcerto(0L, 0L)).isEqualTo(0.0);
        assertThat(support.calcularTaxaAcerto(null, 5L)).isEqualTo(0.0);
        assertThat(support.calcularTaxaAcerto(5L, null)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Deve classificar topico como CRITICO quando taxa < 60% e peso >= 6")
    void deveClassificarComoCriticoPorPeso() {
        NivelCriticidadeTopico criticidade = support.determinarCriticidade(
                10L, 50.0, 7, NivelProficiencia.INTERMEDIARIO
        );
        assertThat(criticidade).isEqualTo(NivelCriticidadeTopico.CRITICO);
    }

    @Test
    @DisplayName("Deve classificar topico como CRITICO quando taxa < 60% e proficiencia for INICIANTE")
    void deveClassificarComoCriticoPorProficienciaIniciante() {
        NivelCriticidadeTopico criticidade = support.determinarCriticidade(
                5L, 40.0, 1, NivelProficiencia.INICIANTE
        );
        assertThat(criticidade).isEqualTo(NivelCriticidadeTopico.CRITICO);
    }

    @Test
    @DisplayName("Deve classificar topico como ATENCAO quando taxa entre 60% e 74.9%")
    void deveClassificarComoAtencao() {
        NivelCriticidadeTopico criticidade = support.determinarCriticidade(
                10L, 65.0, 2, NivelProficiencia.INTERMEDIARIO
        );
        assertThat(criticidade).isEqualTo(NivelCriticidadeTopico.ATENCAO);
    }

    @Test
    @DisplayName("Deve classificar topico como ATENCAO quando taxa >= 75% porem com menos de 3 tentativas")
    void deveClassificarComoAtencaoBaixaAmostragem() {
        NivelCriticidadeTopico criticidade = support.determinarCriticidade(
                2L, 100.0, 3, NivelProficiencia.AVANCADO
        );
        assertThat(criticidade).isEqualTo(NivelCriticidadeTopico.ATENCAO);
    }

    @Test
    @DisplayName("Deve classificar topico como ESTAVEL quando taxa >= 75% e 3 ou mais tentativas")
    void deveClassificarComoEstavel() {
        NivelCriticidadeTopico criticidade = support.determinarCriticidade(
                10L, 85.0, 3, NivelProficiencia.AVANCADO
        );
        assertThat(criticidade).isEqualTo(NivelCriticidadeTopico.ESTAVEL);
    }

    @Test
    @DisplayName("Deve classificar topico como SEM_DADOS quando nao houver tentativas")
    void deveClassificarComoSemDados() {
        assertThat(support.determinarCriticidade(0L, 0.0, 5, NivelProficiencia.INICIANTE))
                .isEqualTo(NivelCriticidadeTopico.SEM_DADOS);
        assertThat(support.determinarCriticidade(null, 0.0, 5, NivelProficiencia.INICIANTE))
                .isEqualTo(NivelCriticidadeTopico.SEM_DADOS);
    }

    @Test
    @DisplayName("Deve calcular indice de severidade ponderado")
    void deveCalcularIndiceSeveridade() {
        // Peso 4, taxa 40% -> severidade = 4 * (100 - 40) = 240.0
        double severidade = support.calcularIndiceSeveridade(40.0, 4, 10L);
        assertThat(severidade).isEqualTo(240.0);

        // Sem tentativas: severidade = 0.0
        double severidadeSemTentativas = support.calcularIndiceSeveridade(0.0, 3, 0L);
        assertThat(severidadeSemTentativas).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Deve calcular Readiness Index ponderado pelo peso dos topicos")
    void deveCalcularReadinessIndex() {
        TopicoLacunaResponse t1 = new TopicoLacunaResponse(
                UUID.randomUUID(), "T1", 2, NivelProficiencia.INTERMEDIARIO,
                10L, 8L, 80.0, 60.0, NivelCriticidadeTopico.ESTAVEL, true
        );
        TopicoLacunaResponse t2 = new TopicoLacunaResponse(
                UUID.randomUUID(), "T2", 4, NivelProficiencia.INICIANTE,
                10L, 5L, 50.0, 90.0, NivelCriticidadeTopico.CRITICO, false
        );

        // (80 * 2 + 50 * 4) / (2 + 4) = (160 + 200) / 6 = 360 / 6 = 60.0
        double score = support.calcularReadinessIndex(List.of(t1, t2));
        assertThat(score).isEqualTo(60.0);
    }

    @Test
    @DisplayName("Deve retornar 0.0 para Readiness Index com lista vazia")
    void deveRetornarZeroReadinessIndexListaVazia() {
        assertThat(support.calcularReadinessIndex(List.of())).isEqualTo(0.0);
        assertThat(support.calcularReadinessIndex(null)).isEqualTo(0.0);
    }
}
