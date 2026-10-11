package com.kyofoundation.skillnapse.modules.cronograma.dto.response;

import com.kyofoundation.skillnapse.modules.cronograma.enums.DiaSemana;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record BlocoHorarioResponse(
        UUID id,
        UUID templateSemanalId,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        TipoBloco tipoBloco,
        String titulo,
        UUID materiaId,
        String materiaNome,
        UUID topicoId,
        String topicoTitulo,
        List<TopicoRevisaoItemResponse> topicosRevisao
) {
    public BlocoHorarioResponse(
            UUID id,
            UUID templateSemanalId,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFim,
            TipoBloco tipoBloco,
            UUID materiaId,
            String materiaNome,
            UUID topicoId,
            String topicoTitulo
    ) {
        this(id, templateSemanalId, diaSemana, horaInicio, horaFim, tipoBloco, topicoTitulo != null ? topicoTitulo : materiaNome, materiaId, materiaNome, topicoId, topicoTitulo, List.of());
    }

    public BlocoHorarioResponse(
            UUID id,
            UUID templateSemanalId,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFim,
            TipoBloco tipoBloco,
            UUID materiaId,
            String materiaNome,
            UUID topicoId,
            String topicoTitulo,
            List<TopicoRevisaoItemResponse> topicosRevisao
    ) {
        this(id, templateSemanalId, diaSemana, horaInicio, horaFim, tipoBloco, topicoTitulo != null ? topicoTitulo : materiaNome, materiaId, materiaNome, topicoId, topicoTitulo, topicosRevisao);
    }
}
