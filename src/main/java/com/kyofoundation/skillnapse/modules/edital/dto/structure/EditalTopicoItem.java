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
        List<String> subtopicos
) {
}
