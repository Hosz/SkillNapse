package com.kyofoundation.skillnapse.modules.planoestudo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Payload contendo a lista ordenada de identificadores de itens para reordenação")
public record ReordenarItensRequest(
        @NotEmpty(message = "A lista de identificadores ordenados não pode estar vazia.")
        @Schema(description = "Lista sequencial de IDs na nova ordem desejada")
        List<UUID> idsOrdenados
) {
}
