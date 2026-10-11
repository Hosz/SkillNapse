package com.kyofoundation.skillnapse.modules.edital.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EditalCargoSupportTest {

    private EditalCargoSupport editalCargoSupport;

    @BeforeEach
    void setUp() {
        editalCargoSupport = new EditalCargoSupport();
    }

    @Test
    @DisplayName("Deve retornar fallback 'Geral' quando entradas forem nulas ou vazias")
    void deveLidarComEntradasNulas() {
        assertThat(editalCargoSupport.enriquecerCargosSeNecessario(null, null)).containsExactly("Geral");
        assertThat(editalCargoSupport.enriquecerCargosSeNecessario("texto", null)).containsExactly("Geral");
        assertThat(editalCargoSupport.enriquecerCargosSeNecessario(null, List.of("Cargo 1"))).containsExactly("Cargo 1");
    }

    @Test
    @DisplayName("Deve sanitizar prefixos como 'Cargo 01:', numeração e pontuações")
    void deveSanitizarPrefixosEPontuacoes() {
        List<String> cargos = List.of(
                "Cargo 01: Analista Judiciário - Área Judiciária",
                "Cargo 02 - Técnico Judiciário",
                "1. Auditor-Fiscal",
                "\"Especialidade: Tecnologia da Informação\""
        );

        List<String> resultado = editalCargoSupport.enriquecerCargosSeNecessario(null, cargos);

        assertThat(resultado).containsExactly(
                "Analista Judiciário - Área Judiciária",
                "Técnico Judiciário",
                "Auditor-Fiscal",
                "Tecnologia da Informação"
        );
    }

    @Test
    @DisplayName("Deve descartar cabeçalhos inválidos e remover duplicatas case-insensitive")
    void deveDescartarCabecalhosInvalidosEDuplicidades() {
        List<String> cargos = List.of(
                "ANEXO I - CRONOGRAMA",
                "Analista Judiciário",
                "analista judiciário",
                "QUADRO DE VAGAS",
                "CONTEÚDO PROGRAMÁTICO"
        );

        List<String> resultado = editalCargoSupport.enriquecerCargosSeNecessario(null, cargos);

        assertThat(resultado).containsExactly("Analista Judiciário");
    }

    @Test
    @DisplayName("Deve enriquecer com cargos identificados por padrão textual no edital")
    void deveEnriquecerComCargosPorPadraoTextual() {
        String texto = """
                EDITAL Nº 01/2026 - CONCURSO PÚBLICO
                Cargo 01: Analista de Sistemas
                Cargo 02: Engenheiro Civil
                Cargo 03: Contador
                """;
        List<String> cargos = List.of("Analista de Sistemas");

        List<String> resultado = editalCargoSupport.enriquecerCargosSeNecessario(texto, cargos);

        assertThat(resultado).containsExactly(
                "Analista de Sistemas",
                "Engenheiro Civil",
                "Contador"
        );
    }

    @Test
    @DisplayName("Deve manter lista sem duplicidades quando cargos já estiverem presentes")
    void deveManterListaSemDuplicidades() {
        String texto = "Cargo 01: Agente Federal";
        List<String> cargos = List.of("Agente Federal", "Escrivão Federal");

        List<String> resultado = editalCargoSupport.enriquecerCargosSeNecessario(texto, cargos);

        assertThat(resultado).containsExactly("Agente Federal", "Escrivão Federal");
    }
}
