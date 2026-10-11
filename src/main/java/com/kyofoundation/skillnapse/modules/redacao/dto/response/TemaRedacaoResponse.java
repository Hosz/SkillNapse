package com.kyofoundation.skillnapse.modules.redacao.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TemaRedacaoResponse(
        UUID id,
        UUID planoEstudoId,
        String titulo,
        String descricao,
        String textosMotivadores,
        String criteriosAvaliacao,
        Boolean geradoPorIa,
        Instant criadoEm,
        Long totalSubmissoes
) {
}
