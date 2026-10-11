package com.kyofoundation.skillnapse.modules.flashcard.dto.payload;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lote estruturado de flashcards gerados por Inteligência Artificial")
public record LoteFlashcardsIaPayload(

        @Schema(description = "Lista de cartões sintetizados pela IA")
        List<FlashcardGeradoIaPayload> flashcards
) {
}
