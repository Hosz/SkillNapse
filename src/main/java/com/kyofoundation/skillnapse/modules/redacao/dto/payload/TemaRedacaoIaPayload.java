package com.kyofoundation.skillnapse.modules.redacao.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TemaRedacaoIaPayload(
        String titulo,
        String descricao,
        String textosMotivadores,
        String criteriosAvaliacao
) {
}
