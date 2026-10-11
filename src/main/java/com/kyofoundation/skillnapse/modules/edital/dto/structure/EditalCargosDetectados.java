package com.kyofoundation.skillnapse.modules.edital.dto.structure;

import java.util.List;

public record EditalCargosDetectados(
        String nomeConcurso,
        List<String> cargos
) {
}
