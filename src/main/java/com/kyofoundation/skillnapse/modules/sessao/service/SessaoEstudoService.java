package com.kyofoundation.skillnapse.modules.sessao.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.gamificacao.service.OfensivaService;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.sessao.dto.request.RegistrarSessaoEstudoRequest;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.ResumoHorasLiquidasResponse;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.SessaoEstudoResponse;
import com.kyofoundation.skillnapse.modules.sessao.entity.SessaoEstudo;
import com.kyofoundation.skillnapse.modules.sessao.enums.StatusSessaoEstudo;
import com.kyofoundation.skillnapse.modules.sessao.finder.SessaoEstudoFinder;
import com.kyofoundation.skillnapse.modules.sessao.mapper.SessaoEstudoMapper;
import com.kyofoundation.skillnapse.modules.sessao.repository.SessaoEstudoRepository;
import com.kyofoundation.skillnapse.modules.sessao.repository.TotalizadorSessaoProjection;
import com.kyofoundation.skillnapse.modules.sessao.support.ResumoHorasLiquidasSupport;
import com.kyofoundation.skillnapse.modules.sessao.validator.SessaoEstudoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessaoEstudoService {

    private final UserFinder userFinder;
    private final TopicoFinder topicoFinder;
    private final SessaoEstudoFinder sessaoEstudoFinder;

    private final UsuarioValidator usuarioValidator;
    private final SessaoEstudoValidator sessaoEstudoValidator;

    private final SessaoEstudoRepository sessaoEstudoRepository;
    private final ResumoHorasLiquidasSupport resumoHorasLiquidasSupport;
    private final OfensivaService ofensivaService;

    @Transactional
    public SessaoEstudoResponse registrarSessaoEstudo(UUID userId, RegistrarSessaoEstudoRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Topico topico = topicoFinder.findById(request.topicoId());
        sessaoEstudoValidator.validarRegistro(request, usuario, topico);

        SessaoEstudo sessaoEstudo = SessaoEstudoMapper.toEntity(request, usuario, topico);
        SessaoEstudo salva = sessaoEstudoRepository.save(sessaoEstudo);

        if (salva.getStatus() == StatusSessaoEstudo.CONCLUIDA && salva.getIniciadoEm() != null) {
            LocalDate dataEstudo = salva.getIniciadoEm().atZone(ZoneId.systemDefault()).toLocalDate();
            ofensivaService.registrarEstudoSilencioso(usuario, dataEstudo);
        }

        return SessaoEstudoMapper.toResponse(salva);
    }

    @Transactional(readOnly = true)
    public Page<SessaoEstudoResponse> listarHistoricoSessoesEstudo(UUID userId, UUID topicoId, Instant de, Instant ate, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        if (topicoId != null) {
            Topico topico = topicoFinder.findById(topicoId);
            sessaoEstudoValidator.validarTopicoPertenceUsuario(usuario, topico);
        }
        sessaoEstudoValidator.validarFiltroPeriodo(de, ate);

        Page<SessaoEstudo> sessoesEstudo = sessaoEstudoFinder.buscarComFiltros(userId, topicoId, de, ate, pageable);
        return sessoesEstudo.map(SessaoEstudoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SessaoEstudoResponse visualizarSessaoEstudo(UUID userId, UUID sessaoId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        SessaoEstudo sessaoEstudo = sessaoEstudoFinder.findById(sessaoId);
        sessaoEstudoValidator.validarPropriedadeSessao(usuario, sessaoEstudo);

        return SessaoEstudoMapper.toResponse(sessaoEstudo);
    }

    @Transactional(readOnly = true)
    public ResumoHorasLiquidasResponse obterResumoHoras(UUID userId, UUID topicoId, Instant de, Instant ate) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        if (topicoId != null) {
            Topico topico = topicoFinder.findById(topicoId);
            sessaoEstudoValidator.validarTopicoPertenceUsuario(usuario, topico);
        }
        sessaoEstudoValidator.validarFiltroPeriodo(de, ate);

        TotalizadorSessaoProjection dadosBrutos = sessaoEstudoRepository.obterDadosAgregados(userId, topicoId, de, ate);
        return resumoHorasLiquidasSupport.calcularResumo(dadosBrutos);
    }

    @Transactional(readOnly = true)
    public ResumoHorasLiquidasResponse obterResumoHoje(UUID userId, String timezone) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        ZoneId zone = resumoHorasLiquidasSupport.resolverZoneId(timezone);
        LocalDate hoje = LocalDate.now(zone);
        Instant de = hoje.atStartOfDay(zone).toInstant();
        Instant ate = hoje.atTime(LocalTime.MAX).atZone(zone).toInstant();

        TotalizadorSessaoProjection dadosBrutos = sessaoEstudoRepository.obterDadosAgregados(userId, null, de, ate);
        return resumoHorasLiquidasSupport.calcularResumo(dadosBrutos);
    }

    @Transactional
    public void apagarSessaoEstudo(UUID userId, UUID sessaoId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        SessaoEstudo sessaoEstudo = sessaoEstudoFinder.findById(sessaoId);
        sessaoEstudoValidator.validarPropriedadeSessao(usuario, sessaoEstudo);

        sessaoEstudoRepository.delete(sessaoEstudo);
    }
}
