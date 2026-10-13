package com.kyofoundation.skillnapse.modules.planoestudo.dto.response;

import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.RelevanciaBanca;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicoResponse(
        UUID id,
        UUID materiaId,
        UUID topicoPaiId,
        String titulo,
        NivelProficiencia nivelProficiencia,
        Integer pesoEdital,
        RelevanciaBanca relevanciaBanca,
        String justificativaPeso,
        Boolean concluido,
        Integer ordem,
        Instant criadoEm,
        Instant atualizadoEm,
        List<TopicoResponse> subtopicos
) {
    public TopicoResponse(
            UUID id,
            UUID materiaId,
            UUID topicoPaiId,
            String titulo,
            NivelProficiencia nivelProficiencia,
            Integer pesoEdital,
            Boolean concluido,
            Integer ordem,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this(id, materiaId, topicoPaiId, titulo, nivelProficiencia, pesoEdital, null, null, concluido, ordem, criadoEm, atualizadoEm, List.of());
    }

    public TopicoResponse(
            UUID id,
            UUID materiaId,
            UUID topicoPaiId,
            String titulo,
            NivelProficiencia nivelProficiencia,
            Integer pesoEdital,
            Boolean concluido,
            Integer ordem,
            Instant criadoEm,
            Instant atualizadoEm,
            List<TopicoResponse> subtopicos
    ) {
        this(id, materiaId, topicoPaiId, titulo, nivelProficiencia, pesoEdital, null, null, concluido, ordem, criadoEm, atualizadoEm, subtopicos);
    }
}
