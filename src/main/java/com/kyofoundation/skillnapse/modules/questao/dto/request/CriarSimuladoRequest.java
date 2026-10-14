package com.kyofoundation.skillnapse.modules.questao.dto.request;

import com.kyofoundation.skillnapse.modules.questao.enums.TipoSimulado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarSimuladoRequest(
        @NotBlank(message = "O título do simulado é obrigatório.")
        @Size(max = 300, message = "O título deve ter no máximo 300 caracteres.")
        String titulo,

        TipoSimulado tipo
) {
}
