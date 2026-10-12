package com.kyofoundation.skillnapse.modules.redacao.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SugestaoReescritaIaPayload(
        String trechoOriginal,
        String trechoSugerido,
        String justificativa
) {
}
