package com.kyofoundation.skillnapse.modules.gamificacao.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.request.AtualizarMetaDiariaRequest;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.MetaDiariaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.PainelGamificacaoResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.ProgressoMetaDiariaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.StatusOfensivaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.entity.MetaDiaria;
import com.kyofoundation.skillnapse.modules.gamificacao.finder.MetaDiariaFinder;
import com.kyofoundation.skillnapse.modules.gamificacao.mapper.GamificacaoMapper;
import com.kyofoundation.skillnapse.modules.gamificacao.repository.MetaDiariaRepository;
import com.kyofoundation.skillnapse.modules.gamificacao.support.ProgressoMetaDiariaSupport;
import com.kyofoundation.skillnapse.modules.gamificacao.validator.MetaDiariaValidator;
import com.kyofoundation.skillnapse.modules.questao.repository.TentativaQuestaoRepository;
import com.kyofoundation.skillnapse.modules.sessao.repository.SessaoEstudoRepository;
import com.kyofoundation.skillnapse.modules.sessao.repository.TotalizadorSessaoProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetaDiariaService {

    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final MetaDiariaFinder metaDiariaFinder;
    private final MetaDiariaValidator metaDiariaValidator;
    private final MetaDiariaRepository metaDiariaRepository;
    private final SessaoEstudoRepository sessaoEstudoRepository;
    private final TentativaQuestaoRepository tentativaQuestaoRepository;
    private final ProgressoMetaDiariaSupport progressoMetaDiariaSupport;
    private final OfensivaService ofensivaService;

    @Transactional(readOnly = true)
    public MetaDiariaResponse obterConfiguracaoMetas(UUID usuarioId) {
        Usuario usuario = userFinder.findById(usuarioId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        MetaDiaria meta = metaDiariaFinder.buscarOuCriar(usuario);
        return GamificacaoMapper.toMetaResponse(meta);
    }

    @Transactional
    public MetaDiariaResponse atualizarMetas(UUID usuarioId, AtualizarMetaDiariaRequest request) {
        Usuario usuario = userFinder.findById(usuarioId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        metaDiariaValidator.validarAtualizacao(request);

        MetaDiaria meta = metaDiariaFinder.buscarOuCriar(usuario);
        GamificacaoMapper.aplicarAtualizacaoMeta(meta, request);
        MetaDiaria salva = metaDiariaRepository.save(meta);

        return GamificacaoMapper.toMetaResponse(salva);
    }

    @Transactional(readOnly = true)
    public ProgressoMetaDiariaResponse obterProgressoDiario(UUID usuarioId, LocalDate dataReferencia) {
        Usuario usuario = userFinder.findById(usuarioId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        MetaDiaria meta = metaDiariaFinder.buscarOuCriar(usuario);

        LocalDate data = dataReferencia != null ? dataReferencia : LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant de = data.atStartOfDay(zone).toInstant();
        Instant ate = data.atTime(LocalTime.MAX).atZone(zone).toInstant();

        TotalizadorSessaoProjection resumo = sessaoEstudoRepository.obterDadosAgregados(usuarioId, null, de, ate);
        long segundosEstudados = (resumo != null && resumo.getTotalSegundos() != null) ? resumo.getTotalSegundos() : 0L;
        long questoesRespondidas = tentativaQuestaoRepository.contarQuestoesRespondidasNoIntervalo(usuarioId, de, ate);

        return progressoMetaDiariaSupport.calcularProgresso(meta, segundosEstudados, questoesRespondidas, data);
    }

    @Transactional
    public PainelGamificacaoResponse obterPainelCompleto(UUID usuarioId, LocalDate dataReferencia) {
        Usuario usuario = userFinder.findById(usuarioId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        StatusOfensivaResponse ofensiva = ofensivaService.obterStatusOfensiva(usuarioId);
        ProgressoMetaDiariaResponse progresso = obterProgressoDiario(usuarioId, dataReferencia);
        return GamificacaoMapper.toPainelResponse(ofensiva, progresso);
    }
}
