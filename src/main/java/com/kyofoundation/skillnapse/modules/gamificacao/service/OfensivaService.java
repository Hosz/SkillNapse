package com.kyofoundation.skillnapse.modules.gamificacao.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.ProgressoMetaDiariaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.StatusOfensivaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.entity.MetaDiaria;
import com.kyofoundation.skillnapse.modules.gamificacao.entity.OfensivaUsuario;
import com.kyofoundation.skillnapse.modules.gamificacao.finder.MetaDiariaFinder;
import com.kyofoundation.skillnapse.modules.gamificacao.finder.OfensivaUsuarioFinder;
import com.kyofoundation.skillnapse.modules.gamificacao.mapper.GamificacaoMapper;
import com.kyofoundation.skillnapse.modules.gamificacao.repository.OfensivaUsuarioRepository;
import com.kyofoundation.skillnapse.modules.gamificacao.support.CalculoOfensivaSupport;
import com.kyofoundation.skillnapse.modules.gamificacao.support.ProgressoMetaDiariaSupport;
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
public class OfensivaService {

    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final OfensivaUsuarioFinder ofensivaUsuarioFinder;
    private final OfensivaUsuarioRepository ofensivaUsuarioRepository;
    private final CalculoOfensivaSupport calculoOfensivaSupport;
    private final MetaDiariaFinder metaDiariaFinder;
    private final SessaoEstudoRepository sessaoEstudoRepository;
    private final TentativaQuestaoRepository tentativaQuestaoRepository;
    private final ProgressoMetaDiariaSupport progressoMetaDiariaSupport;

    @Transactional
    public StatusOfensivaResponse obterStatusOfensiva(UUID usuarioId) {
        Usuario usuario = userFinder.findById(usuarioId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        LocalDate hoje = LocalDate.now();
        OfensivaUsuario ofensiva = sincronizarOfensivaComMetas(usuario, hoje);

        boolean estudouHoje = calculoOfensivaSupport.isEstudouHoje(ofensiva, hoje);
        boolean ofensivaAtiva = calculoOfensivaSupport.isOfensivaAtiva(ofensiva, hoje);

        return GamificacaoMapper.toStatusResponse(ofensiva, estudouHoje, ofensivaAtiva);
    }

    @Transactional
    public StatusOfensivaResponse registrarEstudo(UUID usuarioId, LocalDate dataEstudo) {
        Usuario usuario = userFinder.findById(usuarioId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        LocalDate data = dataEstudo != null ? dataEstudo : LocalDate.now();
        OfensivaUsuario salva = sincronizarOfensivaComMetas(usuario, data);

        boolean estudouHoje = calculoOfensivaSupport.isEstudouHoje(salva, data);
        boolean ofensivaAtiva = calculoOfensivaSupport.isOfensivaAtiva(salva, data);

        return GamificacaoMapper.toStatusResponse(salva, estudouHoje, ofensivaAtiva);
    }

    @Transactional
    public void registrarEstudoSilencioso(Usuario usuario, LocalDate dataEstudo) {
        if (usuario == null) {
            return;
        }
        usuarioValidator.validarUsuarioAtivo(usuario);
        sincronizarOfensivaComMetas(usuario, dataEstudo);
    }

    @Transactional
    public OfensivaUsuario sincronizarOfensivaComMetas(Usuario usuario, LocalDate dataReferencia) {
        if (usuario == null) {
            return null;
        }
        usuarioValidator.validarUsuarioAtivo(usuario);

        LocalDate data = dataReferencia != null ? dataReferencia : LocalDate.now();
        OfensivaUsuario ofensiva = ofensivaUsuarioFinder.buscarOuCriar(usuario);

        calculoOfensivaSupport.revalidarStreak(ofensiva, data);

        MetaDiaria meta = metaDiariaFinder.buscarOuCriar(usuario);
        ZoneId zone = ZoneId.systemDefault();
        Instant de = data.atStartOfDay(zone).toInstant();
        Instant ate = data.atTime(LocalTime.MAX).atZone(zone).toInstant();

        TotalizadorSessaoProjection resumo = sessaoEstudoRepository.obterDadosAgregados(usuario.getId(), null, de, ate);
        long segundosEstudados = (resumo != null && resumo.getTotalSegundos() != null) ? resumo.getTotalSegundos() : 0L;
        long questoesRespondidas = tentativaQuestaoRepository.contarQuestoesRespondidasNoIntervalo(usuario.getId(), de, ate);

        ProgressoMetaDiariaResponse progresso = progressoMetaDiariaSupport.calcularProgresso(
                meta,
                segundosEstudados,
                questoesRespondidas,
                data
        );

        if (progressoMetaDiariaSupport.isMetaDiariaCumprida(progresso)) {
            calculoOfensivaSupport.registrarEstudo(ofensiva, data);
        }

        return ofensivaUsuarioRepository.save(ofensiva);
    }
}
