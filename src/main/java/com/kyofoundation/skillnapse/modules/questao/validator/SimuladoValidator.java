package com.kyofoundation.skillnapse.modules.questao.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarSimuladoRequest;
import com.kyofoundation.skillnapse.modules.questao.entity.Simulado;
import org.springframework.stereotype.Component;

@Component
public class SimuladoValidator {

    private static final int MAX_TITULO_LENGTH = 300;

    public void validarCriacao(CriarSimuladoRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados de criação do simulado não podem ser nulos.");
        }
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new BadRequestException("O título do simulado é obrigatório.");
        }
        if (request.titulo().trim().length() > MAX_TITULO_LENGTH) {
            throw new BadRequestException("O título do simulado não pode ultrapassar " + MAX_TITULO_LENGTH + " caracteres.");
        }
    }

    public void validarPropriedade(Usuario usuario, Simulado simulado) {
        if (simulado == null || simulado.getUsuario() == null ||
                !simulado.getUsuario().getId().equals(usuario.getId())) {
            throw new ForbiddenException("Você não possui permissão para acessar ou modificar este simulado.");
        }
    }

    public void validarConclusao(Simulado simulado) {
        if (Boolean.TRUE.equals(simulado.getConcluido())) {
            throw new BadRequestException("O simulado já foi concluído anteriormente.");
        }
    }
}
