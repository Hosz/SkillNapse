package com.kyofoundation.skillnapse.modules.edital.dto.structure;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EditalMaterialItem(
        @NotBlank
        @JsonAlias({"materia", "disciplina", "modulo", "titulo", "disciplinaNome"})
        String nome,

        @NotEmpty
        @JsonAlias({"assuntos", "temas", "conteudos", "itens", "topicosList"})
        List<EditalTopicoItem> topicos
) {
}
