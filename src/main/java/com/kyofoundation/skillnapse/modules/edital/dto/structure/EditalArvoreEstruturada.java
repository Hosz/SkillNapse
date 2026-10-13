package com.kyofoundation.skillnapse.modules.edital.dto.structure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EditalArvoreEstruturada(
        @NotBlank
        String nomeConcurso,

        String banca,

        String carreira,

        @NotBlank
        String cargo,

        @NotEmpty
        List<EditalMaterialItem> materias
) {
    public EditalArvoreEstruturada(
            String nomeConcurso,
            String cargo,
            List<EditalMaterialItem> materias
    ) {
        this(nomeConcurso, null, null, cargo, materias);
    }
}
