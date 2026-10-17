package com.kyofoundation.skillnapse.modules.flashcard.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.AtualizarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.CriarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.RevisarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.FlashcardResponse;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.HistoricoRevisaoResponse;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Baralho;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Flashcard;
import com.kyofoundation.skillnapse.modules.flashcard.entity.HistoricoRevisaoFlashcard;
import com.kyofoundation.skillnapse.modules.flashcard.finder.BaralhoFinder;
import com.kyofoundation.skillnapse.modules.flashcard.finder.FlashcardFinder;
import com.kyofoundation.skillnapse.modules.flashcard.finder.HistoricoRevisaoFinder;
import com.kyofoundation.skillnapse.modules.flashcard.mapper.FlashcardMapper;
import com.kyofoundation.skillnapse.modules.flashcard.repository.FlashcardRepository;
import com.kyofoundation.skillnapse.modules.flashcard.repository.HistoricoRevisaoFlashcardRepository;
import com.kyofoundation.skillnapse.modules.flashcard.support.SrsAlgorithmSupport;
import com.kyofoundation.skillnapse.modules.flashcard.validator.BaralhoValidator;
import com.kyofoundation.skillnapse.modules.flashcard.validator.FlashcardValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final UserFinder userFinder;
    private final BaralhoFinder baralhoFinder;
    private final TopicoFinder topicoFinder;
    private final FlashcardFinder flashcardFinder;
    private final HistoricoRevisaoFinder historicoRevisaoFinder;

    private final UsuarioValidator usuarioValidator;
    private final BaralhoValidator baralhoValidator;
    private final FlashcardValidator flashcardValidator;

    private final SrsAlgorithmSupport srsAlgorithmSupport;
    private final FlashcardRepository flashcardRepository;
    private final HistoricoRevisaoFlashcardRepository historicoRevisaoRepository;

    @Transactional
    public FlashcardResponse criarFlashcard(UUID userId, CriarFlashcardRequest request) {
        flashcardValidator.validarCriacao(request);

        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Baralho baralho = baralhoFinder.findById(request.baralhoId());
        baralhoValidator.validarPropriedadeBaralho(usuario, baralho);

        Topico topico = request.topicoId() != null ? topicoFinder.findById(request.topicoId()) : null;
        flashcardValidator.validarTopicoPertenceUsuario(usuario, topico);

        Flashcard entity = FlashcardMapper.toEntity(request, baralho, topico);
        Flashcard salvo = flashcardRepository.save(entity);

        return FlashcardMapper.toResponse(salvo);
    }

    @Transactional
    public FlashcardResponse atualizarFlashcard(UUID userId, UUID flashcardId, AtualizarFlashcardRequest request) {
        flashcardValidator.validarAtualizacao(request);

        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Flashcard flashcard = flashcardFinder.findById(flashcardId);
        flashcardValidator.validarPropriedadeFlashcard(usuario, flashcard);

        Topico topico = request.topicoId() != null ? topicoFinder.findById(request.topicoId()) : null;
        flashcardValidator.validarTopicoPertenceUsuario(usuario, topico);

        FlashcardMapper.updateEntity(flashcard, request, topico);
        Flashcard salvo = flashcardRepository.save(flashcard);

        return FlashcardMapper.toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public FlashcardResponse obterFlashcard(UUID userId, UUID flashcardId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Flashcard flashcard = flashcardFinder.findById(flashcardId);
        flashcardValidator.validarPropriedadeFlashcard(usuario, flashcard);

        return FlashcardMapper.toResponse(flashcard);
    }

    @Transactional(readOnly = true)
    public Page<FlashcardResponse> listarFlashcardsPorBaralho(UUID userId, UUID baralhoId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Baralho baralho = baralhoFinder.findById(baralhoId);
        baralhoValidator.validarPropriedadeBaralho(usuario, baralho);

        Page<Flashcard> flashcards = flashcardFinder.buscarPorBaralho(baralhoId, pageable);
        return flashcards.map(FlashcardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<FlashcardResponse> listarFlashcards(UUID userId, UUID baralhoId, UUID topicoId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        if (baralhoId != null) {
            Baralho baralho = baralhoFinder.findById(baralhoId);
            baralhoValidator.validarPropriedadeBaralho(usuario, baralho);
        }

        if (topicoId != null) {
            Topico topico = topicoFinder.findById(topicoId);
            flashcardValidator.validarTopicoPertenceUsuario(usuario, topico);
        }

        Page<Flashcard> cards = flashcardFinder.buscarCardsUsuario(userId, baralhoId, topicoId, pageable);
        return cards.map(FlashcardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<FlashcardResponse> listarCardsVencidos(UUID userId, UUID baralhoId, UUID topicoId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        if (baralhoId != null) {
            Baralho baralho = baralhoFinder.findById(baralhoId);
            baralhoValidator.validarPropriedadeBaralho(usuario, baralho);
        }

        if (topicoId != null) {
            Topico topico = topicoFinder.findById(topicoId);
            flashcardValidator.validarTopicoPertenceUsuario(usuario, topico);
        }

        Page<Flashcard> vencidos = flashcardFinder.buscarCardsVencidos(userId, LocalDate.now(), baralhoId, topicoId, pageable);
        return vencidos.map(FlashcardMapper::toResponse);
    }

    @Transactional
    public FlashcardResponse revisarFlashcard(UUID userId, UUID flashcardId, RevisarFlashcardRequest request) {
        flashcardValidator.validarRevisao(request);

        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Flashcard flashcard = flashcardFinder.findById(flashcardId);
        flashcardValidator.validarPropriedadeFlashcard(usuario, flashcard);

        SrsAlgorithmSupport.SrsResultadoCalculo resultado = srsAlgorithmSupport.calcularProximaRevisao(
                flashcard, request.classificacao(), LocalDate.now()
        );

        FlashcardMapper.aplicarResultadoSrs(flashcard, resultado);
        Flashcard salvo = flashcardRepository.save(flashcard);

        HistoricoRevisaoFlashcard historico = FlashcardMapper.toHistoricoEntity(salvo, usuario, request);
        historicoRevisaoRepository.save(historico);

        return FlashcardMapper.toResponse(salvo);
    }

    @Transactional
    public void apagarFlashcard(UUID userId, UUID flashcardId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Flashcard flashcard = flashcardFinder.findById(flashcardId);
        flashcardValidator.validarPropriedadeFlashcard(usuario, flashcard);

        flashcardRepository.delete(flashcard);
    }

    @Transactional(readOnly = true)
    public Page<HistoricoRevisaoResponse> listarHistoricoRevisoes(UUID userId, UUID flashcardId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Flashcard flashcard = flashcardFinder.findById(flashcardId);
        flashcardValidator.validarPropriedadeFlashcard(usuario, flashcard);

        Page<HistoricoRevisaoFlashcard> historicos = historicoRevisaoFinder.buscarPorFlashcard(flashcardId, pageable);
        return historicos.map(FlashcardMapper::toHistoricoResponse);
    }
}
