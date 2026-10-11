package com.kyofoundation.skillnapse.modules.planoestudo.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.AtualizarProgressoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EditarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TopicoValidator {

    private static final int MAX_TITULO_LENGTH = 1000;
    private static final int MIN_PESO = 1;
    private static final int MAX_PESO = 10;

    public void validarCriacao(CriarTopicoRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados do tópico não podem ser nulos.");
        }
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new BadRequestException("O título do tópico é obrigatório.");
        }
        if (request.titulo().trim().length() > MAX_TITULO_LENGTH) {
            throw new BadRequestException("O título do tópico não pode ter mais de " + MAX_TITULO_LENGTH + " caracteres.");
        }
        if (request.pesoEdital() != null && (request.pesoEdital() < MIN_PESO || request.pesoEdital() > MAX_PESO)) {
            throw new BadRequestException("O peso no edital deve ser um valor entre " + MIN_PESO + " e " + MAX_PESO + ".");
        }
        if (request.ordem() != null && request.ordem() < 0) {
            throw new BadRequestException("A ordem do tópico não pode ser negativa.");
        }
    }

    public void validarEdicao(EditarTopicoRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados de edição não podem ser nulos.");
        }
        boolean temTitulo = request.titulo() != null && !request.titulo().isBlank();
        boolean temPeso = request.pesoEdital() != null;
        boolean temNivel = request.nivelProficiencia() != null;
        boolean temConcluido = request.concluido() != null;
        boolean temOrdem = request.ordem() != null;

        if (!temTitulo && !temPeso && !temNivel && !temConcluido && !temOrdem) {
            throw new BadRequestException("Pelo menos um campo deve ser informado para atualização.");
        }

        if (request.titulo() != null) {
            if (request.titulo().isBlank()) {
                throw new BadRequestException("O título do tópico não pode ser vazio.");
            }
            if (request.titulo().trim().length() > MAX_TITULO_LENGTH) {
                throw new BadRequestException("O título do tópico não pode ter mais de " + MAX_TITULO_LENGTH + " caracteres.");
            }
        }

        if (request.pesoEdital() != null && (request.pesoEdital() < MIN_PESO || request.pesoEdital() > MAX_PESO)) {
            throw new BadRequestException("O peso no edital deve ser um valor entre " + MIN_PESO + " e " + MAX_PESO + ".");
        }

        if (request.ordem() != null && request.ordem() < 0) {
            throw new BadRequestException("A ordem do tópico não pode ser negativa.");
        }
    }

    public void validarProgresso(AtualizarProgressoRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados de progresso não podem ser nulos.");
        }
        if (request.concluido() == null && request.nivelProficiencia() == null) {
            throw new BadRequestException("Pelo menos o status de conclusão ou o nível de proficiência deve ser informado.");
        }
    }

    public void validarTopicoPertenceUsuario(Usuario usuario, Topico topico) {
        if (topico == null ||
                topico.getMateria() == null ||
                topico.getMateria().getPlanoEstudo() == null ||
                topico.getMateria().getPlanoEstudo().getUsuario() == null ||
                !usuario.getId().equals(topico.getMateria().getPlanoEstudo().getUsuario().getId())) {
            throw new ForbiddenException("Você não tem permissão para acessar este tópico.");
        }
    }

    public void validarTopicoPaiPertenceMateria(Topico topicoPai, Materia materia) {
        if (topicoPai == null || topicoPai.getMateria() == null ||
                !topicoPai.getMateria().getId().equals(materia.getId())) {
            throw new BadRequestException("O tópico pai deve pertencer à mesma matéria.");
        }
    }
}
