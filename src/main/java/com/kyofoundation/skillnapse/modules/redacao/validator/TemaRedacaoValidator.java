package com.kyofoundation.skillnapse.modules.redacao.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.redacao.dto.payload.TemaRedacaoIaPayload;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.GerarTemaRedacaoRequest;
import org.springframework.stereotype.Component;

@Component
public class TemaRedacaoValidator {

    private static final int TAMANHO_MIN_TITULO = 10;
    private static final int TAMANHO_MAX_TITULO = 200;
    private static final int TAMANHO_MIN_MOTIVADORES = 50;
    private static final int TAMANHO_MIN_CRITERIOS = 20;

    public void validarRequisicao(GerarTemaRedacaoRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados para geração do tema de redação não podem ser nulos.");
        }
        if (request.planoEstudoId() == null && (request.eixoTematico() == null || request.eixoTematico().isBlank())) {
            throw new BadRequestException("Informe ao menos o plano de estudos ou um eixo temático para a redação.");
        }
        if (request.bancaAlvo() != null && request.bancaAlvo().length() > 50) {
            throw new BadRequestException("O nome da banca avaliadora não pode ultrapassar 50 caracteres.");
        }
        if (request.generoTextual() != null && request.generoTextual().length() > 50) {
            throw new BadRequestException("O gênero textual não pode ultrapassar 50 caracteres.");
        }
        if (request.eixoTematico() != null && request.eixoTematico().length() > 200) {
            throw new BadRequestException("O eixo temático não pode ultrapassar 200 caracteres.");
        }
    }

    public void validarMateriaPertencePlano(Materia materia, PlanoEstudo plano) {
        if (materia != null && (materia.getPlanoEstudo() == null || !materia.getPlanoEstudo().getId().equals(plano.getId()))) {
            throw new ForbiddenException("A matéria informada não pertence ao plano de estudo indicado.");
        }
    }

    public void validarTopicoPertencePlano(Topico topico, PlanoEstudo plano) {
        if (topico != null) {
            if (topico.getMateria() == null ||
                    topico.getMateria().getPlanoEstudo() == null ||
                    !topico.getMateria().getPlanoEstudo().getId().equals(plano.getId())) {
                throw new ForbiddenException("O tópico informado não pertence ao plano de estudo indicado.");
            }
        }
    }

    public void validarTopicoPertenceMateria(Topico topico, Materia materia) {
        if (topico != null && materia != null) {
            if (topico.getMateria() == null || !topico.getMateria().getId().equals(materia.getId())) {
                throw new ForbiddenException("O tópico informado não pertence à matéria indicada.");
            }
        }
    }

    public void validarTemaGeradoIa(TemaRedacaoIaPayload payload) {
        if (payload == null) {
            throw new BadRequestException("Não foi possível gerar a proposta de redação com a IA.");
        }
        if (payload.titulo() == null || payload.titulo().isBlank()) {
            throw new BadRequestException("A proposta gerada pela IA contém um título vazio.");
        }
        String tituloTrim = payload.titulo().trim();
        if (tituloTrim.length() < TAMANHO_MIN_TITULO || tituloTrim.length() > TAMANHO_MAX_TITULO) {
            throw new BadRequestException("O título da redação deve ter entre " + TAMANHO_MIN_TITULO + " e " + TAMANHO_MAX_TITULO + " caracteres.");
        }
        if (payload.textosMotivadores() == null || payload.textosMotivadores().trim().length() < TAMANHO_MIN_MOTIVADORES) {
            throw new BadRequestException("Os textos motivadores gerados pela IA devem conter ao menos " + TAMANHO_MIN_MOTIVADORES + " caracteres de fundamentação.");
        }
        if (payload.criteriosAvaliacao() == null || payload.criteriosAvaliacao().trim().length() < TAMANHO_MIN_CRITERIOS) {
            throw new BadRequestException("Os critérios de avaliação da proposta devem conter ao menos " + TAMANHO_MIN_CRITERIOS + " caracteres de instruções.");
        }
    }
}
