package com.kyofoundation.skillnapse.modules.planoestudo.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PlanoEstudoResponse(
        UUID id,
        String titulo,
        String descricao,
        String banca,
        Boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public PlanoEstudoResponse(
            UUID id,
            String titulo,
            String descricao,
            Boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this(id, titulo, descricao, null, ativo, criadoEm, atualizadoEm);
    }
}
