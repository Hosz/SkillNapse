package com.kyofoundation.skillnapse.modules.questao.dto.payload;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestaoGeradaIaPayload(
        String enunciado,
        @JsonAlias({"explicacaoGabrito", "gabaritoExplicado", "justificativa", "comentario", "explicacao", "gabarito"})
        String explicacaoGabarito,
        String dificuldade,
        List<AlternativaGeradaIaPayload> alternativas
) {
}
