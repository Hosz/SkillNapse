package com.kyofoundation.skillnapse.modules.redacao.dto.payload;

public record TemaRedacaoIaPayload(
        String titulo,
        String descricao,
        String textosMotivadores,
        String criteriosAvaliacao
) {
}
