package com.kyofoundation.skillnapse.modules.cronograma.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.CriarTemplateSemanalRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.EditarTemplateSemanalRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.GradeSemanalResponse;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.TemplateSemanalResponse;
import com.kyofoundation.skillnapse.modules.cronograma.entity.BlocoHorarioTemplate;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import com.kyofoundation.skillnapse.modules.cronograma.finder.BlocoHorarioTemplateFinder;
import com.kyofoundation.skillnapse.modules.cronograma.finder.TemplateSemanalFinder;
import com.kyofoundation.skillnapse.modules.cronograma.mapper.TemplateSemanalMapper;
import com.kyofoundation.skillnapse.modules.cronograma.repository.TemplateSemanalRepository;
import com.kyofoundation.skillnapse.modules.cronograma.validator.TemplateSemanalValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateSemanalService {

    private final UserFinder userFinder;
    private final TemplateSemanalFinder templateSemanalFinder;
    private final BlocoHorarioTemplateFinder blocoHorarioTemplateFinder;
    private final UsuarioValidator usuarioValidator;
    private final TemplateSemanalValidator templateSemanalValidator;
    private final TemplateSemanalRepository templateSemanalRepository;

    @Transactional
    public TemplateSemanalResponse criarTemplate(UUID userId, CriarTemplateSemanalRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);
        templateSemanalValidator.validarCriacao(request);

        boolean deveAtivar = request.ativo() == null || request.ativo();
        if (deveAtivar) {
            desativarTemplatesAtivos(usuario);
        }

        TemplateSemanal templateSemanal = TemplateSemanalMapper.toCriarTemplateSemanal(usuario, request);
        templateSemanalRepository.save(templateSemanal);

        return TemplateSemanalMapper.toResponse(templateSemanal);
    }

    @Transactional(readOnly = true)
    public Page<TemplateSemanalResponse> listarTemplates(UUID userId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        Page<TemplateSemanal> templateSemanais = templateSemanalFinder.findAllByUsuario(usuario, pageable);
        return templateSemanais.map(TemplateSemanalMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TemplateSemanalResponse visualizarTemplateAtivo(UUID userId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanalAtivo = templateSemanalFinder.findAtivoByUsuario(usuario);
        return TemplateSemanalMapper.toResponse(templateSemanalAtivo);
    }

    @Transactional(readOnly = true)
    public TemplateSemanalResponse visualizarTemplate(UUID userId, UUID templateId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);

        return TemplateSemanalMapper.toResponse(templateSemanal);
    }

    @Transactional(readOnly = true)
    public GradeSemanalResponse visualizarGradeSemanal(UUID userId, UUID templateId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);

        List<BlocoHorarioTemplate> blocosHorario = blocoHorarioTemplateFinder.findAllByTemplateSemanal(templateSemanal);
        return TemplateSemanalMapper.toResponseGradeSemanal(templateSemanal, blocosHorario);
    }

    @Transactional
    public TemplateSemanalResponse editarTemplate(UUID userId, UUID templateId, EditarTemplateSemanalRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);
        templateSemanalValidator.validarEdicao(request);

        if (Boolean.TRUE.equals(request.ativo())) {
            desativarTemplatesAtivos(usuario);
        }

        TemplateSemanalMapper.toEditarTemplateSemanal(templateSemanal, request);
        templateSemanalRepository.save(templateSemanal);

        return TemplateSemanalMapper.toResponse(templateSemanal);
    }

    @Transactional
    public TemplateSemanalResponse ativarTemplate(UUID userId, UUID templateId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);

        desativarTemplatesAtivos(usuario);
        templateSemanal.setAtivo(true);
        templateSemanalRepository.save(templateSemanal);

        return TemplateSemanalMapper.toResponse(templateSemanal);
    }

    @Transactional
    public void apagarTemplate(UUID userId, UUID templateId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);

        templateSemanalRepository.delete(templateSemanal);
    }

    private void desativarTemplatesAtivos(Usuario usuario) {
        List<TemplateSemanal> ativos = templateSemanalRepository.findAllByUsuarioAndAtivoTrue(usuario);
        for (TemplateSemanal ativo : ativos) {
            ativo.setAtivo(false);
        }
        if (!ativos.isEmpty()) {
            templateSemanalRepository.saveAll(ativos);
        }
    }
}
