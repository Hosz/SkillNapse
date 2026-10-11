package com.kyofoundation.skillnapse.modules.redacao.service;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.desempenho.validator.DesempenhoValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.redacao.dto.payload.TemaRedacaoIaPayload;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.GerarTemaRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResumoResponse;
import com.kyofoundation.skillnapse.modules.redacao.entity.TemaRedacao;
import com.kyofoundation.skillnapse.modules.redacao.finder.TemaRedacaoFinder;
import com.kyofoundation.skillnapse.modules.redacao.mapper.TemaRedacaoMapper;
import com.kyofoundation.skillnapse.modules.redacao.repository.TemaRedacaoRepository;
import com.kyofoundation.skillnapse.modules.redacao.support.PromptTemaRedacaoSupport;
import com.kyofoundation.skillnapse.modules.redacao.validator.TemaRedacaoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemaRedacaoService {

    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final PlanoEstudoFinder planoEstudoFinder;
    private final DesempenhoValidator desempenhoValidator;
    private final MateriaFinder materiaFinder;
    private final TopicoFinder topicoFinder;
    private final TemaRedacaoValidator temaRedacaoValidator;
    private final TemaRedacaoFinder temaRedacaoFinder;
    private final TemaRedacaoRepository temaRedacaoRepository;
    private final PromptTemaRedacaoSupport promptTemaRedacaoSupport;
    private final AiOrchestratorService aiOrchestratorService;

    @Transactional
    public TemaRedacaoResponse gerarTema(UUID userId, GerarTemaRedacaoRequest request) {
        temaRedacaoValidator.validarRequisicao(request);

        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        PlanoEstudo plano = null;
        if (request.planoEstudoId() != null) {
            plano = planoEstudoFinder.findById(request.planoEstudoId());
            desempenhoValidator.validarPropriedadePlano(usuario, plano);
        }

        Materia materia = null;
        if (request.materiaId() != null && plano != null) {
            materia = materiaFinder.findById(request.materiaId());
            temaRedacaoValidator.validarMateriaPertencePlano(materia, plano);
        }

        Topico topico = null;
        if (request.topicoId() != null && plano != null) {
            topico = topicoFinder.findById(request.topicoId());
            temaRedacaoValidator.validarTopicoPertencePlano(topico, plano);
            if (materia != null) {
                temaRedacaoValidator.validarTopicoPertenceMateria(topico, materia);
            }
        }

        String prompt = promptTemaRedacaoSupport.construirPromptGeracao(
                plano,
                materia,
                topico,
                request.bancaAlvo(),
                request.generoTextualEfetivo(),
                request.eixoTematico()
        );

        PromptRequest promptRequest = new PromptRequest(prompt, null, 0.2, 8192);
        TemaRedacaoIaPayload payload = aiOrchestratorService.generateStructured(promptRequest, TemaRedacaoIaPayload.class);
        temaRedacaoValidator.validarTemaGeradoIa(payload);

        TemaRedacao entity = TemaRedacaoMapper.toEntity(payload, plano);
        TemaRedacao temaSalvo = temaRedacaoRepository.save(entity);

        return TemaRedacaoMapper.toResponse(temaSalvo, 0L);
    }

    @Transactional(readOnly = true)
    public Page<TemaRedacaoResumoResponse> listarPorPlano(UUID userId, UUID planoEstudoId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Page<TemaRedacao> page;
        if (planoEstudoId != null) {
            PlanoEstudo plano = planoEstudoFinder.findById(planoEstudoId);
            desempenhoValidator.validarPropriedadePlano(usuario, plano);
            page = temaRedacaoRepository.findByPlanoEstudoOrderByCriadoEmDesc(plano, pageable);
        } else {
            page = temaRedacaoRepository.findAll(pageable);
        }

        return page.map(tema -> {
            long totalSubmissoes = temaRedacaoRepository.countSubmissoesByTemaId(tema.getId());
            return TemaRedacaoMapper.toResumoResponse(tema, totalSubmissoes);
        });
    }

    @Transactional(readOnly = true)
    public TemaRedacaoResponse obterPorId(UUID userId, UUID temaId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemaRedacao tema = temaRedacaoFinder.findById(temaId);
        if (tema.getPlanoEstudo() != null) {
            desempenhoValidator.validarPropriedadePlano(usuario, tema.getPlanoEstudo());
        }

        long totalSubmissoes = temaRedacaoRepository.countSubmissoesByTemaId(tema.getId());
        return TemaRedacaoMapper.toResponse(tema, totalSubmissoes);
    }
}
