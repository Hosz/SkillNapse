package com.kyofoundation.skillnapse.modules.cronograma.dto.request;

import com.kyofoundation.skillnapse.modules.cronograma.enums.EstrategiaAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Parâmetros para simulação e geração de cronograma inteligente via IA")
public record GerarAutoAgendamentoRequest(
        @NotNull(message = "O ID do plano de estudo é obrigatório.")
        @Schema(description = "Identificador único do plano de estudo de onde virão as matérias e tópicos")
        UUID planoEstudoId,

        @NotEmpty(message = "Informe ao menos uma janela de disponibilidade semanal.")
        @Schema(description = "Lista de janelas horárias disponíveis ao longo da semana")
        List<@Valid JanelaDisponibilidadeRequest> disponibilidades,

        @Min(value = 30, message = "A duração mínima de um bloco de estudo é 30 minutos.")
        @Max(value = 240, message = "A duração máxima de um bloco de estudo é 240 minutos.")
        @Schema(description = "Duração de cada bloco de estudo em minutos", example = "60", defaultValue = "60")
        Integer duracaoBlocoMinutos,

        @Min(value = 0, message = "O intervalo de descanso não pode ser negativo.")
        @Max(value = 60, message = "O intervalo máximo de descanso é 60 minutos.")
        @Schema(description = "Intervalo sugerido de descanso entre blocos em minutos", example = "10", defaultValue = "10")
        Integer intervaloDescansoMinutos,

        @Schema(description = "Indica se blocos de REVISAO devem ser intercalados", defaultValue = "true")
        Boolean incluirRevisao,

        @Schema(description = "Indica se blocos de SIMULADO devem ser programados", defaultValue = "true")
        Boolean incluirSimulado,

        @Schema(description = "Estratégia pedagógica para balanceamento dos tópicos", example = "FOCO_PESO_EDITAL", defaultValue = "FOCO_PESO_EDITAL")
        EstrategiaAgendamento estrategia,

        @Schema(description = "Instruções personalizadas, rotina e particularidades do estudante", example = "Trabalho em regime 12x36, tenho facilidade em Língua Portuguesa e preciso focar mais em Direito Constitucional.")
        String instrucoesPersonalizadas
) {
    public GerarAutoAgendamentoRequest(
            UUID planoEstudoId,
            List<JanelaDisponibilidadeRequest> disponibilidades,
            Integer duracaoBlocoMinutos,
            Integer intervaloDescansoMinutos,
            Boolean incluirRevisao,
            Boolean incluirSimulado,
            EstrategiaAgendamento estrategia) {
        this(planoEstudoId, disponibilidades, duracaoBlocoMinutos, intervaloDescansoMinutos, incluirRevisao, incluirSimulado, estrategia, null);
    }
    public int duracaoBlocoMinutosEfetiva() {
        return duracaoBlocoMinutos != null ? duracaoBlocoMinutos : 60;
    }

    public int intervaloDescansoMinutosEfetivo() {
        return intervaloDescansoMinutos != null ? intervaloDescansoMinutos : 10;
    }

    public boolean incluirRevisaoEfetivo() {
        return incluirRevisao == null || incluirRevisao;
    }

    public boolean incluirSimuladoEfetivo() {
        return incluirSimulado == null || incluirSimulado;
    }

    public EstrategiaAgendamento estrategiaEfetiva() {
        return estrategia != null ? estrategia : EstrategiaAgendamento.FOCO_PESO_EDITAL;
    }
}
