package com.kyofoundation.skillnapse.modules.edital.dto.structure;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record EditalTopicoItem(
        @NotBlank
        String titulo,

        @Min(1) @Max(10)
        Integer pesoEdital,

        String relevanciaBanca,

        String justificativaPeso,

        List<String> subtopicos
) {
    public EditalTopicoItem(
            String titulo,
            Integer pesoEdital,
            List<String> subtopicos
    ) {
        this(titulo, pesoEdital, null, null, subtopicos);
    }
}
