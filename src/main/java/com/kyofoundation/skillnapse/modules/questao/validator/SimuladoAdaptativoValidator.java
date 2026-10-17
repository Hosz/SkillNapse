package com.kyofoundation.skillnapse.modules.questao.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.AlternativaGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.QuestaoGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.request.GerarSimuladoAdaptativoRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimuladoAdaptativoValidator {

    private static final int QTD_MINIMA = 3;
    private static final int QTD_MAXIMA = 90;

    public void validarRequisicao(GerarSimuladoAdaptativoRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados para geração do simulado adaptativo não podem ser nulos.");
        }
        if (request.planoEstudoId() == null) {
            throw new BadRequestException("O ID do plano de estudo é obrigatório.");
        }
        if (request.quantidadeQuestoes() != null &&
                (request.quantidadeQuestoes() < QTD_MINIMA || request.quantidadeQuestoes() > QTD_MAXIMA)) {
            throw new BadRequestException("A quantidade de questões para o simulado adaptativo deve ser entre "
                    + QTD_MINIMA + " e " + QTD_MAXIMA + ".");
        }
    }

    public void validarTopicosPertencemPlano(List<Topico> topicos, PlanoEstudo plano) {
        if (topicos == null || topicos.isEmpty()) {
            throw new BadRequestException("Nenhum tópico válido foi encontrado para geração do simulado adaptativo.");
        }

        for (Topico topico : topicos) {
            if (topico.getMateria() == null ||
                    topico.getMateria().getPlanoEstudo() == null ||
                    !topico.getMateria().getPlanoEstudo().getId().equals(plano.getId())) {
                throw new ForbiddenException("Um ou mais tópicos selecionados não pertencem ao plano de estudo informado.");
            }
        }
    }

    public void validarQuestoesGeradasIa(List<QuestaoGeradaIaPayload> questoes) {
        if (questoes == null || questoes.isEmpty()) {
            throw new BadRequestException("Não foi possível gerar questões com a IA para os tópicos selecionados.");
        }

        for (QuestaoGeradaIaPayload q : questoes) {
            if (q.enunciado() == null || q.enunciado().isBlank()) {
                throw new BadRequestException("A IA gerou uma questão com enunciado vazio.");
            }
            if (q.alternativas() == null || q.alternativas().size() < 2) {
                throw new BadRequestException("A questão gerada pela IA deve conter ao menos 2 alternativas.");
            }
            long corretas = q.alternativas().stream()
                    .filter(a -> Boolean.TRUE.equals(a.correta()))
                    .count();
            if (corretas != 1L) {
                throw new BadRequestException("A questão gerada pela IA deve possuir exatamente uma alternativa correta.");
            }
        }
    }
}
