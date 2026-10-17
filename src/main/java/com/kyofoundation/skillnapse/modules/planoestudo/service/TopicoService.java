package com.kyofoundation.skillnapse.modules.planoestudo.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.AtualizarProgressoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EditarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.ReordenarItensRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.TopicoResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.mapper.TopicoMapper;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.validator.MateriaValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.validator.TopicoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicoService {

    private final UserFinder userFinder;
    private final MateriaFinder materiaFinder;
    private final TopicoFinder topicoFinder;

    private final UsuarioValidator usuarioValidator;
    private final MateriaValidator materiaValidator;
    private final TopicoValidator topicoValidator;

    private final TopicoRepository topicoRepository;

    @Transactional
    public TopicoResponse criarTopico(UUID userId, UUID materiaId, CriarTopicoRequest request) {
        Usuario usuario = userFinder.findById(userId);
        Materia materia = materiaFinder.findById(materiaId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        materiaValidator.validarMateriaPertenceUsuario(usuario, materia);
        topicoValidator.validarCriacao(request);

        Topico topicoPai = null;
        if (request.topicoPaiId() != null) {
            topicoPai = topicoFinder.findById(request.topicoPaiId());
            topicoValidator.validarTopicoPaiPertenceMateria(topicoPai, materia);
        }

        Topico topico = TopicoMapper.toCriarTopico(materia, topicoPai, request);
        topicoRepository.save(topico);

        return TopicoMapper.toResponse(topico);
    }

    @Transactional(readOnly = true)
    public TopicoResponse visualizarTopico(UUID userId, UUID topicoId) {
        Usuario usuario = userFinder.findById(userId);
        Topico topico = topicoFinder.findById(topicoId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        topicoValidator.validarTopicoPertenceUsuario(usuario, topico);

        return TopicoMapper.toResponse(topico);
    }

    @Transactional
    public TopicoResponse editarTopico(UUID userId, UUID topicoId, EditarTopicoRequest request) {
        Usuario usuario = userFinder.findById(userId);
        Topico topico = topicoFinder.findById(topicoId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        topicoValidator.validarTopicoPertenceUsuario(usuario, topico);
        topicoValidator.validarEdicao(request);

        TopicoMapper.toEditarTopico(topico, request);
        topicoRepository.save(topico);

        return TopicoMapper.toResponse(topico);
    }

    @Transactional
    public TopicoResponse atualizarStatusEProficiencia(UUID userId, UUID topicoId, AtualizarProgressoRequest request) {
        Usuario usuario = userFinder.findById(userId);
        Topico topico = topicoFinder.findById(topicoId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        topicoValidator.validarTopicoPertenceUsuario(usuario, topico);
        topicoValidator.validarProgresso(request);

        TopicoMapper.toAtualizarProgresso(topico, request);
        topicoRepository.save(topico);

        return TopicoMapper.toResponse(topico);
    }

    @Transactional
    public void deletarTopico(UUID userId, UUID topicoId) {
        Usuario usuario = userFinder.findById(userId);
        Topico topico = topicoFinder.findById(topicoId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        topicoValidator.validarTopicoPertenceUsuario(usuario, topico);

        topicoRepository.delete(topico);
    }

    @Transactional(readOnly = true)
    public List<TopicoResponse> listarArvoreTopicosPorMateria(UUID userId, UUID materiaId) {
        Usuario usuario = userFinder.findById(userId);
        Materia materia = materiaFinder.findById(materiaId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        materiaValidator.validarMateriaPertenceUsuario(usuario, materia);

        List<Topico> topicosRaiz = topicoRepository.findByMateriaAndTopicoPaiIsNullOrderByOrdemAsc(materia);
        return topicosRaiz.stream().map(TopicoMapper::toResponse).toList();
    }

    @Transactional
    public void reordenarTopicos(UUID userId, UUID materiaId, ReordenarItensRequest request) {
        Usuario usuario = userFinder.findById(userId);
        Materia materia = materiaFinder.findById(materiaId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        materiaValidator.validarMateriaPertenceUsuario(usuario, materia);

        if (request == null || request.idsOrdenados() == null || request.idsOrdenados().isEmpty()) {
            return;
        }

        for (int i = 0; i < request.idsOrdenados().size(); i++) {
            UUID topicoId = request.idsOrdenados().get(i);
            Topico topico = topicoFinder.findById(topicoId);
            topicoValidator.validarTopicoPertenceUsuario(usuario, topico);
            topico.setOrdem(i);
            topicoRepository.save(topico);
        }
    }
}
