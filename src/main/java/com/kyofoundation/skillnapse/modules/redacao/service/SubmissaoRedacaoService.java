package com.kyofoundation.skillnapse.modules.redacao.service;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.redacao.dto.payload.FeedbackCorrecaoIaPayload;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.SubmeterRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.SubmissaoRedacaoResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.SubmissaoRedacaoResumoResponse;
import com.kyofoundation.skillnapse.modules.redacao.entity.SubmissaoRedacao;
import com.kyofoundation.skillnapse.modules.redacao.entity.TemaRedacao;
import com.kyofoundation.skillnapse.modules.redacao.finder.SubmissaoRedacaoFinder;
import com.kyofoundation.skillnapse.modules.redacao.finder.TemaRedacaoFinder;
import com.kyofoundation.skillnapse.modules.redacao.mapper.SubmissaoRedacaoMapper;
import com.kyofoundation.skillnapse.modules.redacao.repository.SubmissaoRedacaoRepository;
import com.kyofoundation.skillnapse.modules.redacao.support.JsonFeedbackRedacaoSupport;
import com.kyofoundation.skillnapse.modules.redacao.support.PromptCorrecaoRedacaoSupport;
import com.kyofoundation.skillnapse.modules.redacao.validator.SubmissaoRedacaoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.kyofoundation.skillnapse.modules.redacao.dto.payload.AvaliacaoLegibilidadeIaPayload;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.ResultadoSubmissaoImagemResponse;
import com.kyofoundation.skillnapse.modules.redacao.support.PromptRedacaoMultimodalSupport;
import com.kyofoundation.skillnapse.modules.redacao.validator.RedacaoImagemUploadValidator;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SubmissaoRedacaoService {

    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final TemaRedacaoFinder temaRedacaoFinder;
    private final SubmissaoRedacaoValidator submissaoRedacaoValidator;
    private final RedacaoImagemUploadValidator redacaoImagemUploadValidator;
    private final SubmissaoRedacaoFinder submissaoRedacaoFinder;
    private final SubmissaoRedacaoRepository submissaoRedacaoRepository;
    private final PromptCorrecaoRedacaoSupport promptCorrecaoRedacaoSupport;
    private final PromptRedacaoMultimodalSupport promptRedacaoMultimodalSupport;
    private final AiOrchestratorService aiOrchestratorService;
    private final JsonFeedbackRedacaoSupport jsonFeedbackRedacaoSupport;

    @Transactional
    public SubmissaoRedacaoResponse submeterRedacao(UUID userId, SubmeterRedacaoRequest request) {
        submissaoRedacaoValidator.validarRequisicao(request);

        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemaRedacao tema = temaRedacaoFinder.findById(request.temaRedacaoId());
        submissaoRedacaoValidator.validarPropriedadeTema(usuario, tema);

        String prompt = promptCorrecaoRedacaoSupport.construirPromptCorrecao(tema, request.textoAluno());
        PromptRequest promptRequest = new PromptRequest(
                prompt,
                "Você é o Auditor-Chefe e Examinador de Banca de Elite para Redações nos padrões ENEM, CEBRASPE, FGV e FCC.",
                0.1,
                4000
        );
        FeedbackCorrecaoIaPayload feedback = aiOrchestratorService.generateStructured(promptRequest, FeedbackCorrecaoIaPayload.class);
        submissaoRedacaoValidator.validarFeedbackIa(feedback);

        String feedbackJson = jsonFeedbackRedacaoSupport.serializar(feedback);
        Instant momento = Instant.now();

        SubmissaoRedacao entity = SubmissaoRedacaoMapper.toEntity(
                request, usuario, tema, feedback, feedbackJson, momento
        );
        SubmissaoRedacao salva = submissaoRedacaoRepository.save(entity);

        return SubmissaoRedacaoMapper.toResponse(salva, feedback);
    }

    @Transactional(readOnly = true)
    public Page<SubmissaoRedacaoResumoResponse> listar(UUID userId, UUID temaRedacaoId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Page<SubmissaoRedacao> page;
        if (temaRedacaoId != null) {
            TemaRedacao tema = temaRedacaoFinder.findById(temaRedacaoId);
            submissaoRedacaoValidator.validarPropriedadeTema(usuario, tema);
            page = submissaoRedacaoRepository.findByUsuarioAndTemaRedacaoOrderByCriadoEmDesc(usuario, tema, pageable);
        } else {
            page = submissaoRedacaoRepository.findByUsuarioOrderByCriadoEmDesc(usuario, pageable);
        }

        return page.map(SubmissaoRedacaoMapper::toResumoResponse);
    }

    @Transactional(readOnly = true)
    public SubmissaoRedacaoResponse obterPorId(UUID userId, UUID id) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        SubmissaoRedacao submissao = submissaoRedacaoFinder.findById(id);
        submissaoRedacaoValidator.validarPropriedadeSubmissao(usuario, submissao);

        FeedbackCorrecaoIaPayload feedback = jsonFeedbackRedacaoSupport.desserializar(submissao.getFeedbackIaJson());
        return SubmissaoRedacaoMapper.toResponse(submissao, feedback);
    }

    @Transactional
    public ResultadoSubmissaoImagemResponse submeterRedacaoImagem(UUID userId, UUID temaRedacaoId, MultipartFile imagem) {
        redacaoImagemUploadValidator.validarImagem(imagem);

        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemaRedacao tema = temaRedacaoFinder.findById(temaRedacaoId);
        submissaoRedacaoValidator.validarPropriedadeTema(usuario, tema);

        PromptRequest promptRequest = promptRedacaoMultimodalSupport.construirPromptMultimodal(tema);
        MimeType mimeType = MimeTypeUtils.parseMimeType(imagem.getContentType());
        Resource mediaResource = imagem.getResource();

        AvaliacaoLegibilidadeIaPayload resultadoIa = aiOrchestratorService.generateStructuredMultimodal(
                promptRequest,
                mimeType,
                mediaResource,
                AvaliacaoLegibilidadeIaPayload.class
        );

        if (resultadoIa == null || !resultadoIa.legivel() ||
                resultadoIa.percentualLegibilidade() == null ||
                resultadoIa.percentualLegibilidade() < PromptRedacaoMultimodalSupport.LIMIAR_LEGIBILIDADE_MINIMO) {
            double percentual = (resultadoIa != null && resultadoIa.percentualLegibilidade() != null)
                    ? resultadoIa.percentualLegibilidade()
                    : 0.0;
            String msg = (resultadoIa != null && resultadoIa.justificativaIlegibilidade() != null && !resultadoIa.justificativaIlegibilidade().isBlank())
                    ? resultadoIa.justificativaIlegibilidade()
                    : "Legibilidade insuficiente (" + percentual + "%). Para garantir uma avaliação justa e precisa, recomendamos redigir ou transcrever o texto manualmente.";
            return new ResultadoSubmissaoImagemResponse(false, percentual, msg, null, null);
        }

        FeedbackCorrecaoIaPayload feedback = resultadoIa.correcao();
        submissaoRedacaoValidator.validarFeedbackIa(feedback);

        String feedbackJson = jsonFeedbackRedacaoSupport.serializar(feedback);
        Instant momento = Instant.now();

        SubmeterRedacaoRequest requestSimulado = new SubmeterRedacaoRequest(
                temaRedacaoId,
                resultadoIa.textoTranscrito() != null ? resultadoIa.textoTranscrito() : ""
        );

        SubmissaoRedacao entity = SubmissaoRedacaoMapper.toEntity(
                requestSimulado, usuario, tema, feedback, feedbackJson, momento
        );
        SubmissaoRedacao salva = submissaoRedacaoRepository.save(entity);

        return new ResultadoSubmissaoImagemResponse(
                true,
                resultadoIa.percentualLegibilidade(),
                "Redação manuscrita transcrita e corrigida com sucesso!",
                resultadoIa.textoTranscrito(),
                SubmissaoRedacaoMapper.toResponse(salva, feedback)
        );
    }

    @Transactional
    public void apagarSubmissao(UUID userId, UUID submissaoId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        SubmissaoRedacao submissao = submissaoRedacaoFinder.findById(submissaoId);
        submissaoRedacaoValidator.validarPropriedadeSubmissao(usuario, submissao);

        submissaoRedacaoRepository.delete(submissao);
    }
}
