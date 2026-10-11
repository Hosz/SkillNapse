package com.kyofoundation.skillnapse.modules.cronograma.dto.response;

import com.kyofoundation.skillnapse.modules.cronograma.enums.DiaSemana;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoAcaoExcecao;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ExcecaoDiariaResponse(
        UUID id,
        LocalDate dataExcecao,
        TipoAcaoExcecao tipoAcao,
        UUID blocoTemplateOrigemId,
        DiaSemana blocoTemplateOrigemDiaSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        TipoBloco tipoBloco,
        UUID materiaId,
        String materiaNome,
        UUID topicoId,
        String topicoTitulo,
        String titulo
) {
    public ExcecaoDiariaResponse(
            UUID id,
            LocalDate dataExcecao,
            TipoAcaoExcecao tipoAcao,
            UUID blocoTemplateOrigemId,
            DiaSemana blocoTemplateOrigemDiaSemana,
            LocalTime horaInicio,
            LocalTime horaFim,
            TipoBloco tipoBloco,
            UUID materiaId,
            String materiaNome,
            UUID topicoId,
            String topicoTitulo
    ) {
        this(id, dataExcecao, tipoAcao, blocoTemplateOrigemId, blocoTemplateOrigemDiaSemana, horaInicio, horaFim, tipoBloco, materiaId, materiaNome, topicoId, topicoTitulo, null);
    }
}
