package com.kyofoundation.skillnapse.modules.flashcard.service;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.flashcard.dto.payload.FlashcardGeradoIaPayload;
import com.kyofoundation.skillnapse.modules.flashcard.dto.payload.LoteFlashcardsIaPayload;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.GerarFlashcardsIaRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.FlashcardResponse;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Baralho;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Flashcard;
import com.kyofoundation.skillnapse.modules.flashcard.mapper.FlashcardMapper;
import com.kyofoundation.skillnapse.modules.flashcard.repository.FlashcardRepository;
import com.kyofoundation.skillnapse.modules.flashcard.support.FlashcardBaralhoResolucaoSupport;
import com.kyofoundation.skillnapse.modules.flashcard.support.PromptFlashcardIaSupport;
import com.kyofoundation.skillnapse.modules.flashcard.validator.FlashcardIaValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardIaService {

    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final PlanoEstudoFinder planoEstudoFinder;
    private final MateriaFinder materiaFinder;
    private final TopicoFinder topicoFinder;
    private final FlashcardBaralhoResolucaoSupport flashcardBaralhoResolucaoSupport;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardIaValidator flashcardIaValidator;
    private final PromptFlashcardIaSupport promptFlashcardIaSupport;
    private final AiOrchestratorService aiOrchestratorService;

    @Transactional
    public List<FlashcardResponse> gerarFlashcardsIa(UUID userId, GerarFlashcardsIaRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);
        flashcardIaValidator.validarRequisicao(request);

        PlanoEstudo plano = planoEstudoFinder.findById(request.planoId());
        flashcardIaValidator.validarPropriedadePlano(usuario, plano);

        Materia materia = request.materiaId() != null ? materiaFinder.findById(request.materiaId()) : null;
        UUID topicoIdEfetivo = request.subtopicoId() != null ? request.subtopicoId() : request.topicoId();
        Topico topico = topicoIdEfetivo != null ? topicoFinder.findById(topicoIdEfetivo) : null;

        Baralho baralho = flashcardBaralhoResolucaoSupport.resolverBaralhoDestino(usuario, materia, plano, request.baralhoId());

        int quantidadeDesejada = request.quantidadeEfetiva();
        String promptTexto = promptFlashcardIaSupport.construirPrompt(
                plano.getTitulo(),
                materia != null ? materia.getNome() : null,
                topico != null ? topico.getTitulo() : null,
                request.subtopico(),
                quantidadeDesejada
        );

        PromptRequest promptRequest = new PromptRequest(
                promptTexto,
                null,
                0.2,
                8192
        );

        log.info("Iniciando síntese de {} flashcards via IA para o usuário {} (Plano: {}, Tópico: {})",
                quantidadeDesejada, usuario.getId(), plano.getTitulo(), topico != null ? topico.getTitulo() : "Geral");

        LoteFlashcardsIaPayload lote = aiOrchestratorService.generateStructured(promptRequest, LoteFlashcardsIaPayload.class);

        if (lote == null || lote.flashcards() == null || lote.flashcards().isEmpty()) {
            log.warn("IA retornou lote vazio de flashcards. Gerando cartão mnemônico de segurança.");
            Flashcard fallback = FlashcardMapper.toEntity(
                    "Conceito central de " + (topico != null ? topico.getTitulo() : plano.getTitulo()),
                    "Revisão dos fundamentos, regras e principais incidências cobradas pelas bancas.",
                    baralho,
                    topico
            );
            return List.of(FlashcardMapper.toResponse(flashcardRepository.save(fallback)));
        }

        List<FlashcardResponse> respostas = new ArrayList<>();
        for (FlashcardGeradoIaPayload item : lote.flashcards()) {
            if (item.frente() == null || item.frente().isBlank() || item.verso() == null || item.verso().isBlank()) {
                continue;
            }
            Flashcard entity = FlashcardMapper.toEntity(item.frente(), item.verso(), baralho, topico);
            Flashcard salvo = flashcardRepository.save(entity);
            respostas.add(FlashcardMapper.toResponse(salvo));
        }

        log.info("Síntese de flashcards concluída com sucesso: {} cards criados no baralho '{}'",
                respostas.size(), baralho.getTitulo());

        return respostas;
    }
}
