package com.kyofoundation.skillnapse.modules.flashcard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Requisição para síntese automatizada de flashcards contextuais via Inteligência Artificial")
public record GerarFlashcardsIaRequest(

        @NotNull(message = "O ID do plano de estudo é obrigatório.")
        @Schema(description = "ID do plano de estudo selecionado", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID planoId,

        @Schema(description = "ID da matéria (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa7")
        UUID materiaId,

        @Schema(description = "ID do tópico (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa8")
        UUID topicoId,

        @Schema(description = "Nome textual do subtópico (opcional)", example = "Atos Administrativos Vinculados e Discricionários")
        String subtopico,

        @Schema(description = "ID do subtópico (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa9")
        UUID subtopicoId,

        @Schema(description = "Quantidade de flashcards desejados (padrão: 10, máximo: 60)", example = "10")
        Integer quantidade,

        @Schema(description = "ID do baralho de destino (opcional, se não informado usa ou cria um baralho adequado)", example = "3fa85f64-5717-4562-b3fc-2c963f66afb0")
        UUID baralhoId
) {

    public int quantidadeEfetiva() {
        if (quantidade == null || quantidade <= 0) {
            return 10;
        }
        return Math.min(quantidade, 60);
    }
}
