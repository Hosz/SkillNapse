package com.kyofoundation.skillnapse.modules.questao.dto.payload;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlternativaGeradaIaPayload(
        String texto,
        @JsonAlias({"correto", "isCorreta", "isCorrect", "certa"})
        Boolean correta,
        @JsonAlias({"letra", "numero", "index", "posicao"})
        Integer ordem
) {
}
