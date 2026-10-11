package com.kyofoundation.skillnapse.modules.cronograma.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.CriarBlocoHorarioRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.EditarBlocoHorarioRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.BlocoHorarioResponse;
import com.kyofoundation.skillnapse.modules.cronograma.entity.BlocoHorarioTemplate;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import com.kyofoundation.skillnapse.modules.cronograma.finder.BlocoHorarioTemplateFinder;
import com.kyofoundation.skillnapse.modules.cronograma.finder.TemplateSemanalFinder;
import com.kyofoundation.skillnapse.modules.cronograma.mapper.BlocoHorarioTemplateMapper;
import com.kyofoundation.skillnapse.modules.cronograma.repository.BlocoHorarioTemplateRepository;
import com.kyofoundation.skillnapse.modules.cronograma.validator.BlocoHorarioTemplateValidator;
import com.kyofoundation.skillnapse.modules.cronograma.validator.TemplateSemanalValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlocoHorarioTemplateService {

    private final UserFinder userFinder;
    private final TemplateSemanalFinder templateSemanalFinder;
    private final BlocoHorarioTemplateFinder blocoHorarioTemplateFinder;
    private final MateriaFinder materiaFinder;
    private final TopicoFinder topicoFinder;
    private final UsuarioValidator usuarioValidator;
    private final TemplateSemanalValidator templateSemanalValidator;
    private final BlocoHorarioTemplateValidator blocoHorarioTemplateValidator;
    private final BlocoHorarioTemplateRepository blocoHorarioTemplateRepository;

    @Transactional
    public BlocoHorarioResponse adicionarBlocoHorario(UUID userId, UUID templateId, CriarBlocoHorarioRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);
        blocoHorarioTemplateValidator.validarCriacao(request, templateSemanal);

        Materia materia = request.materiaId() != null ? materiaFinder.findById(request.materiaId()) : null;
        Topico topico = request.topicoId() != null ? topicoFinder.findById(request.topicoId()) : null;
        blocoHorarioTemplateValidator.validarMateriaETopico(usuario, materia, topico);

        BlocoHorarioTemplate blocoHorarioTemplate = BlocoHorarioTemplateMapper.toCriarBlocoHorario(templateSemanal, request, materia, topico);
        blocoHorarioTemplateRepository.save(blocoHorarioTemplate);

        return BlocoHorarioTemplateMapper.toResponse(blocoHorarioTemplate);
    }

    @Transactional(readOnly = true)
    public Page<BlocoHorarioResponse> listarBlocosHorario(UUID userId, UUID templateId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);

        Page<BlocoHorarioTemplate> blocosHorariosTemplate = blocoHorarioTemplateFinder.findByTemplateSemanal(templateSemanal, pageable);
        return blocosHorariosTemplate.map(BlocoHorarioTemplateMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BlocoHorarioResponse visualizarBlocoHorario(UUID userId, UUID templateId, UUID blocoId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);

        BlocoHorarioTemplate blocoHorarioTemplate = blocoHorarioTemplateFinder.findById(blocoId);
        blocoHorarioTemplateValidator.validarBlocoPertenceTemplate(blocoHorarioTemplate, templateSemanal);

        return BlocoHorarioTemplateMapper.toResponse(blocoHorarioTemplate);
    }

    @Transactional
    public BlocoHorarioResponse editarBlocoHorario(UUID userId, UUID templateId, UUID blocoId, EditarBlocoHorarioRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);

        BlocoHorarioTemplate blocoHorarioTemplate = blocoHorarioTemplateFinder.findById(blocoId);
        blocoHorarioTemplateValidator.validarBlocoPertenceTemplate(blocoHorarioTemplate, templateSemanal);
        blocoHorarioTemplateValidator.validarEdicao(request, blocoHorarioTemplate, templateSemanal);

        Materia materia;
        if (Boolean.TRUE.equals(request.desvincularMateria())) {
            materia = null;
        } else if (request.materiaId() != null) {
            materia = materiaFinder.findById(request.materiaId());
        } else {
            materia = blocoHorarioTemplate.getMateria();
        }

        Topico topico;
        if (Boolean.TRUE.equals(request.desvincularTopico()) || Boolean.TRUE.equals(request.desvincularMateria())) {
            topico = null;
        } else if (request.topicoId() != null) {
            topico = topicoFinder.findById(request.topicoId());
        } else if (materia != null && blocoHorarioTemplate.getMateria() != null && !materia.getId().equals(blocoHorarioTemplate.getMateria().getId())) {
            // Se a matéria foi alterada e nenhum novo tópico foi informado, reseta o tópico anterior
            topico = null;
        } else if (materia == null) {
            topico = null;
        } else {
            topico = blocoHorarioTemplate.getTopico();
        }
        blocoHorarioTemplateValidator.validarMateriaETopico(usuario, materia, topico);

        BlocoHorarioTemplateMapper.toEditarBlocoHorario(blocoHorarioTemplate, request, materia, topico);
        blocoHorarioTemplateRepository.save(blocoHorarioTemplate);

        return BlocoHorarioTemplateMapper.toResponse(blocoHorarioTemplate);
    }

    @Transactional
    public void apagarBlocoHorario(UUID userId, UUID templateId, UUID blocoId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        TemplateSemanal templateSemanal = templateSemanalFinder.findById(templateId);
        templateSemanalValidator.validarTemplatePertenceUsuario(usuario, templateSemanal);

        BlocoHorarioTemplate blocoHorarioTemplate = blocoHorarioTemplateFinder.findById(blocoId);
        blocoHorarioTemplateValidator.validarBlocoPertenceTemplate(blocoHorarioTemplate, templateSemanal);

        blocoHorarioTemplateRepository.delete(blocoHorarioTemplate);
    }
}
