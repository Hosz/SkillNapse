package com.kyofoundation.skillnapse.modules.flashcard.finder;

import com.kyofoundation.skillnapse.common.exception.ResourceNotFoundException;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Flashcard;
import com.kyofoundation.skillnapse.modules.flashcard.repository.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FlashcardFinder {

    private final FlashcardRepository flashcardRepository;

    public Flashcard findById(UUID id) {
        return flashcardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard não encontrado."));
    }

    public Page<Flashcard> buscarPorBaralho(UUID baralhoId, Pageable pageable) {
        return flashcardRepository.findByBaralhoId(baralhoId, pageable);
    }

    public Page<Flashcard> buscarPorTopico(UUID topicoId, Pageable pageable) {
        return flashcardRepository.findByTopicoId(topicoId, pageable);
    }

    public Page<Flashcard> buscarCardsVencidos(UUID usuarioId, LocalDate dataReferencia, UUID baralhoId, UUID topicoId, Pageable pageable) {
        return flashcardRepository.buscarCardsVencidos(usuarioId, dataReferencia, baralhoId, topicoId, pageable);
    }

    public Page<Flashcard> buscarCardsUsuario(UUID usuarioId, UUID baralhoId, UUID topicoId, Pageable pageable) {
        return flashcardRepository.buscarCardsUsuario(usuarioId, baralhoId, topicoId, pageable);
    }

    public long contarCardsVencidos(UUID usuarioId, LocalDate dataReferencia) {
        return flashcardRepository.contarCardsVencidos(usuarioId, dataReferencia);
    }
}
