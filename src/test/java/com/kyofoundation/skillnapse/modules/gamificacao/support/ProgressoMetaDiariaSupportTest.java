package com.kyofoundation.skillnapse.modules.gamificacao.support;

import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.ProgressoMetaDiariaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.entity.MetaDiaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressoMetaDiariaSupportTest {

    private ProgressoMetaDiariaSupport support;
    private MetaDiaria meta;

    @BeforeEach
    void setUp() {
        support = new ProgressoMetaDiariaSupport();
        meta = MetaDiaria.builder()
                .id(UUID.randomUUID())
                .metaMinutosEstudo(120)
                .metaQuestoesResolvidas(20)
                .build();
    }

    @Test
    @DisplayName("[calcularProgresso] Deve calcular progresso parcial corretamente")
    void deveCalcularProgressoParcial() {
        LocalDate hoje = LocalDate.of(2026, 10, 8);
        long segundosEstudo = 3600L; // 60 minutos
        long questoesRespondidas = 10L;

        ProgressoMetaDiariaResponse progresso = support.calcularProgresso(meta, segundosEstudo, questoesRespondidas, hoje);

        assertThat(progresso.data()).isEqualTo(hoje);
        assertThat(progresso.metaMinutosEstudo()).isEqualTo(120);
        assertThat(progresso.minutosEstudadosHoje()).isEqualTo(60L);
        assertThat(progresso.percentualMinutosEstudo()).isEqualTo(50.0);
        assertThat(progresso.metaMinutosAtingida()).isFalse();

        assertThat(progresso.metaQuestoesResolvidas()).isEqualTo(20);
        assertThat(progresso.questoesResolvidasHoje()).isEqualTo(10L);
        assertThat(progresso.percentualQuestoes()).isEqualTo(50.0);
        assertThat(progresso.metaQuestoesAtingida()).isFalse();

        assertThat(progresso.todasMetasAtingidas()).isFalse();
    }

    @Test
    @DisplayName("[calcularProgresso] Deve reconhecer quando todas as metas forem atingidas")
    void deveReconhecerMetasAtingidas() {
        LocalDate hoje = LocalDate.of(2026, 10, 8);
        long segundosEstudo = 7200L; // 120 minutos
        long questoesRespondidas = 25L; // > 20

        ProgressoMetaDiariaResponse progresso = support.calcularProgresso(meta, segundosEstudo, questoesRespondidas, hoje);

        assertThat(progresso.minutosEstudadosHoje()).isEqualTo(120L);
        assertThat(progresso.metaMinutosAtingida()).isTrue();
        assertThat(progresso.percentualMinutosEstudo()).isEqualTo(100.0);

        assertThat(progresso.questoesResolvidasHoje()).isEqualTo(25L);
        assertThat(progresso.metaQuestoesAtingida()).isTrue();
        assertThat(progresso.percentualQuestoes()).isEqualTo(100.0);

        assertThat(progresso.todasMetasAtingidas()).isTrue();
    }

    @Test
    @DisplayName("[calcularProgresso] Deve calcular corretamente com valores zerados")
    void deveCalcularComValoresZerados() {
        LocalDate hoje = LocalDate.of(2026, 10, 8);

        ProgressoMetaDiariaResponse progresso = support.calcularProgresso(meta, 0L, 0L, hoje);

        assertThat(progresso.minutosEstudadosHoje()).isEqualTo(0L);
        assertThat(progresso.percentualMinutosEstudo()).isEqualTo(0.0);
        assertThat(progresso.metaMinutosAtingida()).isFalse();

        assertThat(progresso.questoesResolvidasHoje()).isEqualTo(0L);
        assertThat(progresso.percentualQuestoes()).isEqualTo(0.0);
        assertThat(progresso.metaQuestoesAtingida()).isFalse();

        assertThat(progresso.todasMetasAtingidas()).isFalse();
    }

    @Test
    @DisplayName("[calcularProgresso] Deve considerar meta de questões opcional se configurada como 0")
    void deveConsiderarMetaQuestoesOpcionalQuandoZero() {
        meta.setMetaQuestoesResolvidas(0);
        LocalDate hoje = LocalDate.of(2026, 10, 8);

        ProgressoMetaDiariaResponse progresso = support.calcularProgresso(meta, 7200L, 0L, hoje);

        assertThat(progresso.metaMinutosAtingida()).isTrue();
        assertThat(progresso.metaQuestoesAtingida()).isTrue();
        assertThat(progresso.todasMetasAtingidas()).isTrue();
        assertThat(support.isMetaDiariaCumprida(progresso)).isTrue();
    }
}
