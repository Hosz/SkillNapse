package com.kyofoundation.skillnapse.modules.questao.mapper;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarSimuladoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.SimuladoResponse;
import com.kyofoundation.skillnapse.modules.questao.entity.Simulado;
import com.kyofoundation.skillnapse.modules.questao.enums.TipoSimulado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

@Component
public class SimuladoMapper {

    public static Simulado toEntity(CriarSimuladoRequest request, Usuario usuario) {
        if (request == null) {
            return null;
        }

        TipoSimulado tipo = request.tipo() != null ? request.tipo() : TipoSimulado.MANUAL;

        return Simulado.builder()
                .usuario(usuario)
                .titulo(request.titulo().trim())
                .tipo(tipo)
                .concluido(false)
                .tentativas(new ArrayList<>())
                .build();
    }

    public static SimuladoResponse toResponse(Simulado simulado, long totalQuestoes, long totalAcertos) {
        return toResponseComQuestoes(simulado, totalQuestoes, totalAcertos, java.util.List.of());
    }

    public static SimuladoResponse toResponseComQuestoes(
            Simulado simulado,
            long totalQuestoes,
            long totalAcertos,
            java.util.List<com.kyofoundation.skillnapse.modules.questao.dto.response.QuestaoItemSimuladoResponse> questoes
    ) {
        if (simulado == null) {
            return null;
        }

        double percentual = 0.0;
        if (totalQuestoes > 0) {
            percentual = BigDecimal.valueOf(((double) totalAcertos / totalQuestoes) * 100.0)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new SimuladoResponse(
                simulado.getId(),
                simulado.getTitulo(),
                simulado.getTipo(),
                simulado.getConcluido(),
                simulado.getCriadoEm(),
                totalQuestoes,
                totalAcertos,
                percentual,
                questoes != null ? questoes : java.util.List.of()
        );
    }
}
