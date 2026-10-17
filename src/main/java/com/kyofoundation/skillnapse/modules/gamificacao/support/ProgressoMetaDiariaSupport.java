package com.kyofoundation.skillnapse.modules.gamificacao.support;

import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.ProgressoMetaDiariaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.entity.MetaDiaria;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ProgressoMetaDiariaSupport {

    public ProgressoMetaDiariaResponse calcularProgresso(
            MetaDiaria meta,
            long segundosEstudadosHoje,
            long questoesRespondidasHoje,
            LocalDate data) {

        int metaMinutos = (meta != null && meta.getMetaMinutosEstudo() != null) ? meta.getMetaMinutosEstudo() : 120;
        int metaQuestoes = (meta != null && meta.getMetaQuestoesResolvidas() != null) ? meta.getMetaQuestoesResolvidas() : 0;

        long minutosEstudados = segundosEstudadosHoje / 60;

        double percMinutos = metaMinutos > 0
                ? Math.min(100.0, Math.round(((double) minutosEstudados / metaMinutos * 100.0) * 10.0) / 10.0)
                : 100.0;
        boolean metaMinutosAtingida = minutosEstudados >= metaMinutos;

        double percQuestoes = metaQuestoes > 0
                ? Math.min(100.0, Math.round(((double) questoesRespondidasHoje / metaQuestoes * 100.0) * 10.0) / 10.0)
                : 100.0;
        boolean metaQuestoesAtingida = metaQuestoes <= 0 || questoesRespondidasHoje >= metaQuestoes;

        boolean todasAtingidas = metaMinutosAtingida && metaQuestoesAtingida;

        return new ProgressoMetaDiariaResponse(
                data != null ? data : LocalDate.now(),
                metaMinutos,
                minutosEstudados,
                percMinutos,
                metaMinutosAtingida,
                metaQuestoes,
                questoesRespondidasHoje,
                percQuestoes,
                metaQuestoesAtingida,
                todasAtingidas
        );
    }

    public boolean isMetaDiariaCumprida(ProgressoMetaDiariaResponse progresso) {
        if (progresso == null) {
            return false;
        }
        return progresso.todasMetasAtingidas();
    }
}
