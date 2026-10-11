package com.kyofoundation.skillnapse.modules.cronograma.dto.request;

import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoAcaoExcecao;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record RegistrarExcecaoDiariaRequest(
        @NotNull(message = "O tipo de ação da exceção é obrigatório.")
        @Schema(description = "Tipo de ação da exceção", example = "SUBSTITUIR_HORARIO")
        TipoAcaoExcecao tipoAcao,

        @Schema(description = "ID do bloco do template de origem (obrigatório para CANCELAR_BLOCO ou SUBSTITUIR_HORARIO)")
        UUID blocoTemplateOrigem,

        @Schema(description = "Novo horário de início (obrigatório para SUBSTITUIR_HORARIO ou BLOCO_AVULSO)", example = "15:00:00")
        LocalTime horaInicio,

        @Schema(description = "Novo horário de término (obrigatório para SUBSTITUIR_HORARIO ou BLOCO_AVULSO)", example = "16:30:00")
        LocalTime horaFim,

        @Schema(description = "Tipo de bloco de estudo", example = "REVISAO")
        TipoBloco tipoBloco,

        @Schema(description = "ID opcional da matéria associada")
        UUID materiaId,

        @Schema(description = "ID opcional do tópico específico associado")
        UUID topicoId,

        @Schema(description = "Título personalizado do bloco", example = "Revisão Geral")
        String titulo
) {
    public RegistrarExcecaoDiariaRequest(
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
