package com.kyofoundation.skillnapse.modules.flashcard.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.GerarFlashcardsIaRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import org.springframework.stereotype.Component;

@Component
public class FlashcardIaValidator {

    public void validarRequisicao(GerarFlashcardsIaRequest request) {
        if (request == null) {
            throw new BadRequestException("O corpo da requisição de geração de flashcards não pode ser nulo.");
        }
        if (request.planoId() == null) {
            throw new BadRequestException("O ID do plano de estudo é obrigatório.");
        }
    }

    public void validarPropriedadePlano(Usuario usuario, PlanoEstudo plano) {
        if (plano == null || plano.getUsuario() == null || usuario == null) {
            throw new BadRequestException("Dados inválidos para validação do plano de estudo.");
        }
        if (!plano.getUsuario().getId().equals(usuario.getId())) {
            throw new ForbiddenException("Você não tem permissão para gerar flashcards vinculados a este plano de estudos.");
        }
    }
}
