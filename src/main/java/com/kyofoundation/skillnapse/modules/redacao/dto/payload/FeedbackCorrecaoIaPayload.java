package com.kyofoundation.skillnapse.modules.redacao.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FeedbackCorrecaoIaPayload(
        Double notaGeral,
        List<AvaliacaoCompetenciaIaPayload> competencias,
        String comentariosGerais,
        List<SugestaoReescritaIaPayload> sugestoesReescrita
) {
}
