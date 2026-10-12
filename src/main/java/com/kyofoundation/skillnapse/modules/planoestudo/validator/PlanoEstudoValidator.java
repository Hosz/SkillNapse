package com.kyofoundation.skillnapse.modules.planoestudo.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EdicaoPlanoEstudoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.PlanoEstudoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanoEstudoValidator {

    private static final int MAX_TITULO_LENGTH = 255;

    public void validarCriacao(PlanoEstudoRequest request) {
        if (request == null) {
            throw new BadRequestException("O plano de estudo não pode ser vazio.");
        }
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new BadRequestException("O título não pode ser vazio.");
        }
        if (request.titulo().trim().length() > MAX_TITULO_LENGTH) {
            throw new BadRequestException("O título não pode ter mais de " + MAX_TITULO_LENGTH + " caracteres.");
        }
    }

    public void validarEdicao(EdicaoPlanoEstudoRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados de edição não podem ser nulos.");
        }
        boolean temTitulo = request.titulo() != null && !request.titulo().isBlank();
        boolean temDescricao = request.descricao() != null;
        boolean temAtivo = request.ativo() != null;

        if (!temTitulo && !temDescricao && !temAtivo) {
            throw new BadRequestException("Pelo menos um campo deve ser informado para atualização.");
        }
        if (request.titulo() != null) {
            if (request.titulo().isBlank()) {
                throw new BadRequestException("O título não pode ser vazio.");
            }
            if (request.titulo().trim().length() > MAX_TITULO_LENGTH) {
                throw new BadRequestException("O título não pode ter mais de " + MAX_TITULO_LENGTH + " caracteres.");
            }
        }
    }

    public void validarPlanoPertenceUsuario(Usuario usuario, PlanoEstudo planoEstudo) {
        if (!usuario.getId().equals(planoEstudo.getUsuario().getId())) {
            throw new ForbiddenException("O plano de estudo não pertence à esse usuário.");
        }
    }
}
