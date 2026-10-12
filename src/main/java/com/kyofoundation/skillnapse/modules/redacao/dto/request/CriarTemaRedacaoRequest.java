package com.kyofoundation.skillnapse.modules.redacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Payload para criação manual de uma proposta de redação dissertativa")
public record CriarTemaRedacaoRequest(

        @Schema(description = "ID do plano de estudo associado (opcional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID planoEstudoId,

        @NotBlank(message = "O título da proposta de redação é obrigatório.")
        @Size(min = 5, max = 200, message = "O título deve ter entre 5 e 200 caracteres.")
        @Schema(description = "Título da proposta de redação", example = "O Impacto da Inteligência Artificial no Mercado de Trabalho")
        String titulo,

        @Size(max = 1000, message = "A descrição não pode ultrapassar 1000 caracteres.")
        @Schema(description = "Descrição e orientações gerais do tema", example = "Redija um texto dissertativo-argumentativo sobre os desafios éticos e regulatórios.")
        String descricao,

        @NotBlank(message = "Os textos motivadores são obrigatórios.")
        @Size(min = 20, message = "Os textos motivadores devem conter ao menos 20 caracteres.")
        @Schema(description = "Fragmentos, dados ou textos motivadores de apoio", example = "Texto 1: A revolução da IA generativa acelera transformações no mercado profissional...")
        String textosMotivadores,

        @Size(max = 2000, message = "Os critérios de avaliação não podem ultrapassar 2000 caracteres.")
        @Schema(description = "Critérios específicos de correção ou padrão da banca", example = "Avaliação com ênfase em coesão textual, proposta de intervenção e repertório.")
        String criteriosAvaliacao
) {
}
