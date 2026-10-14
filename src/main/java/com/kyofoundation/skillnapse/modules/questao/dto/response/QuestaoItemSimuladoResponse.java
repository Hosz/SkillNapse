package com.kyofoundation.skillnapse.modules.questao.dto.response;

import com.kyofoundation.skillnapse.modules.questao.enums.DificuldadeQuestao;

import java.util.List;
import java.util.UUID;

public record QuestaoItemSimuladoResponse(
        UUID id,
        UUID topicoId,
        String assuntoGeral,
        String topicoReferencia,
        String enunciado,
        DificuldadeQuestao dificuldade,
        String banca,
        Integer ano,
        List<AlternativaItemSimuladoResponse> alternativas,
        UUID alternativaCorretaId,
        String letraCorreta,
        UUID alternativaEscolhidaId,
        String letraEscolhida,
        Boolean acertou,
        String explicacaoGabarito
) {
    public QuestaoItemSimuladoResponse(
            UUID id,
            UUID topicoId,
            String assuntoGeral,
            String topicoReferencia,
            String enunciado,
            DificuldadeQuestao dificuldade,
            String banca,
            Integer ano,
            List<AlternativaItemSimuladoResponse> alternativas
    ) {
        this(id, topicoId, assuntoGeral, topicoReferencia, enunciado, dificuldade, banca, ano, alternativas, null, null, null, null, null, null);
    }
}

