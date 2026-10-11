package com.kyofoundation.skillnapse.modules.cronograma.dto.request;

import com.kyofoundation.skillnapse.modules.cronograma.enums.DiaSemana;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record CriarBlocoHorarioRequest(
        @NotNull(message = "O dia da semana é obrigatório.")
        @Schema(description = "Dia da semana do bloco", example = "SEGUNDA")
        DiaSemana diaSemana,

        @NotNull(message = "O horário de início é obrigatório.")
        @Schema(description = "Horário de início (formato HH:mm:ss ou HH:mm)", example = "14:00:00")
        LocalTime horaInicio,

        @NotNull(message = "O horário de término é obrigatório.")
        @Schema(description = "Horário de término (formato HH:mm:ss ou HH:mm)", example = "15:30:00")
        LocalTime horaFim,

        @Schema(description = "Tipo de bloco de estudo (padrão: FOCO_TEORIA)", example = "FOCO_TEORIA")
        TipoBloco tipoBloco,

        @Schema(description = "Identificador opcional da matéria associada")
        UUID materiaId,

        @Schema(description = "Identificador opcional do tópico específico associado")
        UUID topicoId,

        @Schema(description = "Nome ou título do bloco de estudo", example = "Direito Constitucional - Direitos Fundamentais")
        String titulo
) {
    public CriarBlocoHorarioRequest(
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFim,
            TipoBloco tipoBloco,
            UUID materiaId,
            UUID topicoId
    ) {
        this(diaSemana, horaInicio, horaFim, tipoBloco, materiaId, topicoId, null);
    }
}
