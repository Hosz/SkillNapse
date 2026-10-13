package com.kyofoundation.skillnapse.modules.planoestudo.mapper;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EdicaoPlanoEstudoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.PlanoEstudoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.PlanoEstudoResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import org.springframework.stereotype.Component;

@Component
public class PlanoEstudoMapper {
    public static PlanoEstudo criarPlano(PlanoEstudoRequest request, Usuario usuario) {
        return PlanoEstudo.builder()
                .usuario(usuario)
                .titulo(request.titulo())
                .descricao(request.descricao())
                .ativo(true)
                .build();
    }

    public static PlanoEstudoResponse toResponse(PlanoEstudo planoEstudo) {
        return new PlanoEstudoResponse(
                planoEstudo.getId(),
                planoEstudo.getTitulo(),
                planoEstudo.getDescricao(),
                planoEstudo.getBanca(),
                planoEstudo.getAtivo(),
                planoEstudo.getCriadoEm(),
                planoEstudo.getAtualizadoEm()
        );
    }

    public static void toEditarPlano(PlanoEstudo planoEstudo, EdicaoPlanoEstudoRequest request) {
        if (request.titulo() != null && !request.titulo().isBlank()) {
            planoEstudo.setTitulo(request.titulo());
        }
        if (request.descricao() != null && !request.descricao().isBlank()) {
            planoEstudo.setDescricao(request.descricao());
        }
        if (request.ativo() != null) {
            planoEstudo.setAtivo(request.ativo());
        }
    }
}
