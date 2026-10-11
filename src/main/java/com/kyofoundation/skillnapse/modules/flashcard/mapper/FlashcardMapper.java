package com.kyofoundation.skillnapse.modules.flashcard.mapper;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.AtualizarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.CriarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.RevisarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.FlashcardResponse;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.HistoricoRevisaoResponse;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Baralho;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Flashcard;
import com.kyofoundation.skillnapse.modules.flashcard.entity.HistoricoRevisaoFlashcard;
import com.kyofoundation.skillnapse.modules.flashcard.support.SrsAlgorithmSupport;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class FlashcardMapper {

    public static Flashcard toEntity(CriarFlashcardRequest request, Baralho baralho, Topico topico) {
        return Flashcard.builder()
                .baralho(baralho)
                .topico(topico)
                .frente(request.frente().trim())
                .verso(request.verso().trim())
                .fatorFacilidade(new BigDecimal("2.50"))
                .intervaloDias(0)
                .repeticoes(0)
                .proximaRevisao(LocalDate.now())
                .build();
    }

    public static Flashcard toEntity(String frente, String verso, Baralho baralho, Topico topico) {
        return Flashcard.builder()
                .baralho(baralho)
                .topico(topico)
                .frente(frente != null ? frente.trim() : "")
                .verso(verso != null ? verso.trim() : "")
                .fatorFacilidade(new BigDecimal("2.50"))
                .intervaloDias(0)
                .repeticoes(0)
                .proximaRevisao(LocalDate.now())
                .build();
    }

    public static void updateEntity(Flashcard entity, AtualizarFlashcardRequest request, Topico topico) {
        entity.setFrente(request.frente().trim());
        entity.setVerso(request.verso().trim());
        entity.setTopico(topico);
    }

    public static void aplicarResultadoSrs(Flashcard entity, SrsAlgorithmSupport.SrsResultadoCalculo resultadoSrs) {
        entity.setFatorFacilidade(resultadoSrs.fatorFacilidade());
        entity.setIntervaloDias(resultadoSrs.intervaloDias());
        entity.setRepeticoes(resultadoSrs.repeticoes());
        entity.setProximaRevisao(resultadoSrs.proximaRevisao());
        entity.setUltimaRevisao(Instant.now());
    }

    public static FlashcardResponse toResponse(Flashcard flashcard) {
        if (flashcard == null) {
            return null;
        }

        UUID baralhoId = flashcard.getBaralho() != null ? flashcard.getBaralho().getId() : null;
        String baralhoTitulo = flashcard.getBaralho() != null ? flashcard.getBaralho().getTitulo() : null;
        UUID topicoId = flashcard.getTopico() != null ? flashcard.getTopico().getId() : null;
        String topicoTitulo = flashcard.getTopico() != null ? flashcard.getTopico().getTitulo() : null;

        return new FlashcardResponse(
                flashcard.getId(),
                baralhoId,
                baralhoTitulo,
                topicoId,
                topicoTitulo,
                flashcard.getFrente(),
                flashcard.getVerso(),
                flashcard.getFatorFacilidade(),
                flashcard.getIntervaloDias(),
                flashcard.getRepeticoes(),
                flashcard.getProximaRevisao(),
                flashcard.getUltimaRevisao()
        );
    }

    public static HistoricoRevisaoFlashcard toHistoricoEntity(Flashcard flashcard, Usuario usuario, RevisarFlashcardRequest request) {
        return HistoricoRevisaoFlashcard.builder()
                .flashcard(flashcard)
                .usuario(usuario)
                .classificacaoResposta(request.classificacao())
                .tempoRespostaSegundos(request.tempoRespostaSegundos())
                .revisadoEm(Instant.now())
                .build();
    }

    public static HistoricoRevisaoResponse toHistoricoResponse(HistoricoRevisaoFlashcard historico) {
        if (historico == null) {
            return null;
        }

        UUID flashcardId = historico.getFlashcard() != null ? historico.getFlashcard().getId() : null;

        return new HistoricoRevisaoResponse(
                historico.getId(),
                flashcardId,
                historico.getClassificacaoResposta(),
                historico.getTempoRespostaSegundos(),
                historico.getRevisadoEm()
        );
    }
}
