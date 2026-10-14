package com.kyofoundation.skillnapse.modules.edital.dto.structure;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EditalArvoreEstruturada(
        @NotBlank
        @JsonAlias({"nome_concurso", "concurso", "nomeCertame", "certame", "titulo"})
        String nomeConcurso,

        @NotBlank
        @JsonAlias({"cargoAlvo", "cargo_alvo", "funcao", "especialidade", "perfil"})
        String cargo,

        @NotEmpty
        @JsonAlias({"disciplinas", "conteudoProgramatico", "conteudo_programatico", "gradeCurricular", "subjects", "materiaList"})
        List<EditalMaterialItem> materias
) {
}
