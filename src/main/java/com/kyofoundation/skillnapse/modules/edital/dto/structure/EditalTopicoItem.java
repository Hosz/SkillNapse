package com.kyofoundation.skillnapse.modules.edital.dto.structure;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record EditalTopicoItem(
        @NotBlank
        @JsonAlias({"nome", "assunto", "tema", "item", "topico"})
        String titulo,

        @Min(1) @Max(10)
        @JsonAlias({"peso_edital", "peso", "relevancia", "prioridade"})
        Integer pesoEdital,

        @JsonAlias({"sub_topicos", "itens", "subtopicos", "detalhes", "conteudos", "subitens"})
        List<String> subtopicos
) {
}
