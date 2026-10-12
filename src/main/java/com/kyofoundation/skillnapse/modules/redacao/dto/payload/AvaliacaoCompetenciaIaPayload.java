package com.kyofoundation.skillnapse.modules.redacao.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AvaliacaoCompetenciaIaPayload(
        String nomeCompetencia,
        Double nota,
        String comentarios,
        List<String> desviosIdentificados
) {
}
