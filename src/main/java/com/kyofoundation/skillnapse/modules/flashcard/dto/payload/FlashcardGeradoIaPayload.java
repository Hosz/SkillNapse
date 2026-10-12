package com.kyofoundation.skillnapse.modules.flashcard.dto.payload;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Estrutura atômica de um flashcard gerado por Inteligência Artificial")
public record FlashcardGeradoIaPayload(

        @Schema(description = "Pergunta, conceito gatilho ou problema na frente do cartão",
                example = "Qual é o prazo para impetrar Mandado de Segurança?")
        @JsonAlias({"pergunta", "question", "front"})
        String frente,

        @Schema(description = "Resposta concisa, fundamentação jurídica ou regra mnemônica no verso do cartão",
                example = "120 dias corridos, contados da ciência oficial pelo interessado do ato impugnado (art. 23 da Lei 12.016/2009).")
        @JsonAlias({"resposta", "answer", "back"})
        String verso
) {
}
