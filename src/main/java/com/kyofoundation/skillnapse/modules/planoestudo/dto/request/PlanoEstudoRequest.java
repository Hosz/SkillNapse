package com.kyofoundation.skillnapse.modules.planoestudo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanoEstudoRequest(

        @NotNull(message = "O título é obrigatório.")
        @NotBlank(message = "O título não pode ser vazio.")
        @Size(max = 150, message = "O título não pode ter mais de 150 caracteres.")
        String titulo,

        String descricao
) {
}
