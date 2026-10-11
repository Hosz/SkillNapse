package com.kyofoundation.skillnapse.modules.cronograma.dto.response;

import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoAcaoExcecao;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoOrigemBloco;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record BlocoAgendaDiariaResponse(
        UUID id,
        TipoOrigemBloco origem,
        TipoAcaoExcecao tipoAcaoExcecao,
        UUID blocoTemplateOrigemId,
        UUID excecaoId,
        LocalTime horaInicio,
        LocalTime horaFim,
        TipoBloco tipoBloco,
        UUID materiaId,
        String materiaNome,
        UUID topicoId,
        String topicoTitulo,
        String titulo,
        List<TopicoRevisaoItemResponse> topicosRevisao
) {
    public BlocoAgendaDiariaResponse(
            UUID id,
            TipoOrigemBloco origem,
            TipoAcaoExcecao tipoAcaoExcecao,
            UUID blocoTemplateOrigemId,
            UUID excecaoId,
            LocalTime horaInicio,
            LocalTime horaFim,
            TipoBloco tipoBloco,
            UUID materiaId,
            String materiaNome,
            UUID topicoId,
            String topicoTitulo,
            String titulo
    ) {
        this(id, origem, tipoAcaoExcecao, blocoTemplateOrigemId, excecaoId, horaInicio, horaFim, tipoBloco, materiaId, materiaNome, topicoId, topicoTitulo, titulo, List.of());
    }

    public BlocoAgendaDiariaResponse(
            UUID id,
            TipoOrigemBloco origem,
            TipoAcaoExcecao tipoAcaoExcecao,
            UUID blocoTemplateOrigemId,
            UUID excecaoId,
            LocalTime horaInicio,
            LocalTime horaFim,
            TipoBloco tipoBloco,
            UUID materiaId,
            String materiaNome,
            UUID topicoId,
            String topicoTitulo,
            List<TopicoRevisaoItemResponse> topicosRevisao
    ) {
        this(id, origem, tipoAcaoExcecao, blocoTemplateOrigemId, excecaoId, horaInicio, horaFim, tipoBloco, materiaId, materiaNome, topicoId, topicoTitulo, null, topicosRevisao);
    }

    public BlocoAgendaDiariaResponse(
            UUID id,
            TipoOrigemBloco origem,
            TipoAcaoExcecao tipoAcaoExcecao,
            UUID blocoTemplateOrigemId,
            UUID excecaoId,
            LocalTime horaInicio,
            LocalTime horaFim,
            TipoBloco tipoBloco,
            UUID materiaId,
            String materiaNome,
            UUID topicoId,
            String topicoTitulo
    ) {
        this(id, origem, tipoAcaoExcecao, blocoTemplateOrigemId, excecaoId, horaInicio, horaFim, tipoBloco, materiaId, materiaNome, topicoId, topicoTitulo, null, List.of());
    }
}
