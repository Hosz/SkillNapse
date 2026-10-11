package com.kyofoundation.skillnapse.modules.redacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Requisição para geração automatizada de proposta inédita de redação via IA")
public record GerarTemaRedacaoRequest(

        @Schema(description = "ID do plano de estudo selecionado (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID planoEstudoId,

        @Schema(description = "ID da matéria vinculada (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa7")
        UUID materiaId,

        @Schema(description = "ID do tópico temático (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa8")
        UUID topicoId,

        @Size(max = 50, message = "O nome da banca avaliadora não pode ultrapassar 50 caracteres.")
        @Schema(description = "Estilo da banca examinadora (opcional)", example = "Cebraspe")
        String bancaAlvo,

        @Size(max = 50, message = "O gênero textual não pode ultrapassar 50 caracteres.")
        @Schema(description = "Gênero textual desejado (opcional)", example = "Dissertativo-Argumentativo")
        String generoTextual,

        @Size(max = 200, message = "O eixo temático não pode ultrapassar 200 caracteres.")
        @Schema(description = "Eixo temático livre (opcional)", example = "Direito Constitucional e Políticas Públicas")
        String eixoTematico
) {
    public GerarTemaRedacaoRequest(UUID planoEstudoId, UUID materiaId, UUID topicoId, String bancaAlvo, String generoTextual) {
        this(planoEstudoId, materiaId, topicoId, bancaAlvo, generoTextual, null);
    }

    @jakarta.validation.constraints.AssertTrue(message = "Informe ao menos o plano de estudos ou um eixo temático para a redação.")
    @Schema(hidden = true)
    public boolean isEscopoValido() {
        return planoEstudoId != null || (eixoTematico != null && !eixoTematico.isBlank());
    }

    public String generoTextualEfetivo() {
        return (generoTextual != null && !generoTextual.isBlank())
                ? generoTextual.trim()
                : "Dissertativo-Argumentativo";
    }
}

