package com.kyofoundation.skillnapse.modules.flashcard.mapper;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.AtualizarBaralhoRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.CriarBaralhoRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.BaralhoResponse;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Baralho;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BaralhoMapper {

    public static Baralho toEntity(CriarBaralhoRequest request, Usuario usuario, Materia materia) {
        return Baralho.builder()
                .usuario(usuario)
                .materia(materia)
                .titulo(request.titulo().trim())
                .descricao(request.descricao() != null ? request.descricao().trim() : null)
                .build();
    }

    public static Baralho toEntity(String titulo, String descricao, Usuario usuario, Materia materia) {
        return Baralho.builder()
                .usuario(usuario)
                .materia(materia)
                .titulo(titulo.trim())
                .descricao(descricao != null ? descricao.trim() : null)
                .build();
    }

    public static void updateEntity(Baralho entity, AtualizarBaralhoRequest request, Materia materia) {
        entity.setTitulo(request.titulo().trim());
        entity.setDescricao(request.descricao() != null ? request.descricao().trim() : null);
        entity.setMateria(materia);
    }

    public static BaralhoResponse toResponse(Baralho baralho, Long totalCards, Long totalCardsParaRevisar) {
        if (baralho == null) {
            return null;
        }

        UUID usuarioId = baralho.getUsuario() != null ? baralho.getUsuario().getId() : null;
        UUID materiaId = baralho.getMateria() != null ? baralho.getMateria().getId() : null;
        String materiaNome = baralho.getMateria() != null ? baralho.getMateria().getNome() : null;

        return new BaralhoResponse(
                baralho.getId(),
                usuarioId,
                materiaId,
                materiaNome,
                baralho.getTitulo(),
                baralho.getDescricao(),
                baralho.getCriadoEm(),
                totalCards != null ? totalCards : 0L,
                totalCardsParaRevisar != null ? totalCardsParaRevisar : 0L
        );
    }
}
