package com.kyofoundation.skillnapse.modules.flashcard.support;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Baralho;
import com.kyofoundation.skillnapse.modules.flashcard.finder.BaralhoFinder;
import com.kyofoundation.skillnapse.modules.flashcard.mapper.BaralhoMapper;
import com.kyofoundation.skillnapse.modules.flashcard.repository.BaralhoRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FlashcardBaralhoResolucaoSupport {

    private final BaralhoFinder baralhoFinder;
    private final BaralhoRepository baralhoRepository;

    public Baralho resolverBaralhoDestino(Usuario usuario, Materia materia, PlanoEstudo plano, UUID baralhoIdSolicitado) {
        if (baralhoIdSolicitado != null) {
            return baralhoFinder.findById(baralhoIdSolicitado);
        }

        List<Baralho> baralhosUsuario = baralhoRepository.findByUsuarioId(usuario.getId());

        if (materia != null) {
            for (Baralho b : baralhosUsuario) {
                if (b.getMateria() != null && b.getMateria().getId().equals(materia.getId())) {
                    return b;
                }
            }
            Baralho novoBaralhoMateria = BaralhoMapper.toEntity(
                    "Flashcards - " + materia.getNome(),
                    "Baralho sintetizado automaticamente por IA para a matéria " + materia.getNome(),
                    usuario,
                    materia
            );
            return baralhoRepository.save(novoBaralhoMateria);
        }

        if (!baralhosUsuario.isEmpty()) {
            return baralhosUsuario.getFirst();
        }

        Baralho novoBaralhoGeral = BaralhoMapper.toEntity(
                "Acervo Geral - " + plano.getTitulo(),
                "Baralho principal de memorização ativa para o plano " + plano.getTitulo(),
                usuario,
                null
        );
        return baralhoRepository.save(novoBaralhoGeral);
    }
}
