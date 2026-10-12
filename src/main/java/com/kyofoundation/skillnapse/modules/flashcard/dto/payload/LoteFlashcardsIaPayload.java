package com.kyofoundation.skillnapse.modules.flashcard.dto.payload;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Lote estruturado de flashcards gerados por Inteligência Artificial")
public record LoteFlashcardsIaPayload(

        @Schema(description = "Lista de cartões sintetizados pela IA")
        @JsonAlias({"cards", "listaCards", "itens", "flashcardList"})
        List<FlashcardGeradoIaPayload> flashcards
) {
}
