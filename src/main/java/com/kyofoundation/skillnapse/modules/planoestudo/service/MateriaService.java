package com.kyofoundation.skillnapse.modules.planoestudo.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarMateriaRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EditarMateriaRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.ReordenarItensRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.MateriaResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.mapper.MateriaMapper;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.MateriaRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.validator.MateriaValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.validator.PlanoEstudoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MateriaService {

    private final UserFinder userFinder;
    private final PlanoEstudoFinder planoEstudoFinder;
    private final MateriaFinder materiaFinder;

    private final UsuarioValidator usuarioValidator;
    private final PlanoEstudoValidator planoEstudoValidator;
    private final MateriaValidator materiaValidator;

    private final MateriaRepository materiaRepository;

    @Transactional
    public MateriaResponse criarMateria(UUID userId, UUID planoId, CriarMateriaRequest request) {
        Usuario usuario = userFinder.findById(userId);
        PlanoEstudo planoEstudo = planoEstudoFinder.findById(planoId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        planoEstudoValidator.validarPlanoPertenceUsuario(usuario, planoEstudo);
        materiaValidator.validarCriacao(request);

        Materia materia = MateriaMapper.toCriacaoMateria(request, planoEstudo);
        materiaRepository.save(materia);

        return MateriaMapper.toResponse(materia);
    }

    @Transactional(readOnly = true)
    public MateriaResponse verMateria(UUID userId, UUID materiaId) {
        Usuario usuario = userFinder.findById(userId);
        Materia materia = materiaFinder.findById(materiaId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        materiaValidator.validarMateriaPertenceUsuario(usuario, materia);

        return MateriaMapper.toResponse(materia);
    }

    @Transactional
    public MateriaResponse editarMateria(UUID userId, UUID materiaId, EditarMateriaRequest request) {
        Usuario usuario = userFinder.findById(userId);
        Materia materia = materiaFinder.findById(materiaId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        materiaValidator.validarMateriaPertenceUsuario(usuario, materia);
        materiaValidator.validarEdicao(request);

        MateriaMapper.toEditarMateria(materia, request);
        materiaRepository.save(materia);

        return MateriaMapper.toResponse(materia);
    }

    @Transactional
    public void apagarMateria(UUID userId, UUID materiaId) {
        Usuario usuario = userFinder.findById(userId);
        Materia materia = materiaFinder.findById(materiaId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        materiaValidator.validarMateriaPertenceUsuario(usuario, materia);

        materiaRepository.delete(materia);
    }

    @Transactional(readOnly = true)
    public Page<MateriaResponse> listarMaterias(UUID userId, UUID planoId, Pageable pageable) {
        Usuario usuario = userFinder.findById(userId);
        PlanoEstudo planoEstudo = planoEstudoFinder.findById(planoId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        planoEstudoValidator.validarPlanoPertenceUsuario(usuario, planoEstudo);

        Page<Materia> materias = materiaRepository.findByPlanoEstudo(planoEstudo, pageable);
        return materias.map(MateriaMapper::toResponse);
    }

    @Transactional
    public void reordenarMaterias(UUID userId, UUID planoId, ReordenarItensRequest request) {
        Usuario usuario = userFinder.findById(userId);
        PlanoEstudo planoEstudo = planoEstudoFinder.findById(planoId);

        usuarioValidator.validarUsuarioAtivo(usuario);
        planoEstudoValidator.validarPlanoPertenceUsuario(usuario, planoEstudo);

        if (request == null || request.idsOrdenados() == null || request.idsOrdenados().isEmpty()) {
            return;
        }

        for (int i = 0; i < request.idsOrdenados().size(); i++) {
            UUID materiaId = request.idsOrdenados().get(i);
            Materia materia = materiaFinder.findById(materiaId);
            materiaValidator.validarMateriaPertenceUsuario(usuario, materia);
            materia.setOrdem(i);
            materiaRepository.save(materia);
        }
    }
}
