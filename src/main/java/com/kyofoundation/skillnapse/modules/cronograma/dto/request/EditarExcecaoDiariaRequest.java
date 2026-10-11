package com.kyofoundation.skillnapse.modules.cronograma.dto.request;

import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoAcaoExcecao;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.UUID;

public record EditarExcecaoDiariaRequest(
        @Schema(description = "Tipo de ação da exceção", example = "SUBSTITUIR_HORARIO")
        TipoAcaoExcecao tipoAcao,

        @Schema(description = "ID do bloco do template de origem")
        UUID blocoTemplateOrigem,

        @Schema(description = "Novo horário de início", example = "15:00:00")
        LocalTime horaInicio,

        @Schema(description = "Novo horário de término", example = "16:30:00")
        LocalTime horaFim,

        @Schema(description = "Tipo de bloco de estudo", example = "SIMULADO")
        TipoBloco tipoBloco,

        @Schema(description = "ID da matéria associada")
        UUID materiaId,

        @Schema(description = "ID do tópico associado")
        UUID topicoId,

        @Schema(description = "Título personalizado do bloco", example = "Revisão Geral")
        String titulo
) {
    public EditarExcecaoDiariaRequest(
            TipoAcaoExcecao tipoAcao,
            UUID blocoTemplateOrigem,
            LocalTime horaInicio,
            LocalTime horaFim,
            TipoBloco tipoBloco,
            UUID materiaId,
            UUID topicoId
    ) {
        this(tipoAcao, blocoTemplateOrigem, horaInicio, horaFim, tipoBloco, materiaId, topicoId, null);
    }
}
