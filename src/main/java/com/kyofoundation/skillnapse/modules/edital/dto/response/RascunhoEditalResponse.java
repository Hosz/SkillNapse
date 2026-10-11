package com.kyofoundation.skillnapse.modules.edital.dto.response;

import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalArvoreEstruturada;
import com.kyofoundation.skillnapse.modules.edital.enums.StatusRascunhoEdital;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RascunhoEditalResponse(
        UUID id,
        String nomeArquivo,
        UUID planoEstudoId,
        StatusRascunhoEdital status,
        EditalArvoreEstruturada conteudo,
        List<String> cargosDisponiveis,
        Boolean precisaSelecionarCargo,
        Instant criadoEm
) {
    public RascunhoEditalResponse(
            UUID id,
            String nomeArquivo,
            UUID planoEstudoId,
            StatusRascunhoEdital status,
            EditalArvoreEstruturada conteudo,
            Instant criadoEm
    ) {
        this(id, nomeArquivo, planoEstudoId, status, conteudo, List.of(), false, criadoEm);
    }
}
