package com.kyofoundation.skillnapse.modules.planoestudo.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarMateriaRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EditarMateriaRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MateriaValidator {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6})$");
    private static final int MAX_NOME_LENGTH = 255;

    public void validarCriacao(CriarMateriaRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados da matéria não podem ser nulos.");
        }
        if (request.nome() == null || request.nome().isBlank()) {
            throw new BadRequestException("O nome da matéria é obrigatório.");
        }
        if (request.nome().trim().length() > MAX_NOME_LENGTH) {
            throw new BadRequestException("O nome da matéria não pode ter mais de " + MAX_NOME_LENGTH + " caracteres.");
        }
        if (request.corHex() != null && !request.corHex().isBlank()) {
            validarCorHex(request.corHex());
        }
        if (request.ordem() != null && request.ordem() < 0) {
            throw new BadRequestException("A ordem da matéria não pode ser negativa.");
        }
    }

    public void validarEdicao(EditarMateriaRequest request) {
        if (request == null) {
            throw new BadRequestException("Os dados de edição não podem ser nulos.");
        }
        boolean temNome = request.nome() != null && !request.nome().isBlank();
        boolean temCor = request.corHex() != null && !request.corHex().isBlank();
        boolean temOrdem = request.ordem() != null;

        if (!temNome && !temCor && !temOrdem) {
            throw new BadRequestException("Pelo menos um campo deve ser informado para atualização.");
        }

        if (request.nome() != null) {
            if (request.nome().isBlank()) {
                throw new BadRequestException("O nome da matéria não pode ser vazio.");
            }
            if (request.nome().trim().length() > MAX_NOME_LENGTH) {
                throw new BadRequestException("O nome da matéria não pode ter mais de " + MAX_NOME_LENGTH + " caracteres.");
            }
        }

        if (request.corHex() != null && !request.corHex().isBlank()) {
            validarCorHex(request.corHex());
        }

        if (request.ordem() != null && request.ordem() < 0) {
            throw new BadRequestException("A ordem da matéria não pode ser negativa.");
        }
    }

    public void validarMateriaPertenceUsuario(Usuario usuario, Materia materia) {
        if (materia.getPlanoEstudo() == null ||
                materia.getPlanoEstudo().getUsuario() == null ||
                !usuario.getId().equals(materia.getPlanoEstudo().getUsuario().getId())) {
            throw new ForbiddenException("Você não tem permissão para acessar esta matéria.");
        }
    }

    private void validarCorHex(String corHex) {
        if (!HEX_COLOR_PATTERN.matcher(corHex.trim()).matches()) {
            throw new BadRequestException("A cor deve estar no formato hexadecimal #RRGGBB (ex: #3B82F6).");
        }
    }
}
