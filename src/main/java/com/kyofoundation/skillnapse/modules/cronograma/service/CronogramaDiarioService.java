package com.kyofoundation.skillnapse.modules.cronograma.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.EditarExcecaoDiariaRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.RegistrarExcecaoDiariaRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.AgendaDiariaResponse;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.ExcecaoDiariaResponse;
import com.kyofoundation.skillnapse.modules.cronograma.entity.BlocoHorarioTemplate;
import com.kyofoundation.skillnapse.modules.cronograma.entity.ExcecaoDiaria;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import com.kyofoundation.skillnapse.modules.cronograma.enums.DiaSemana;
import com.kyofoundation.skillnapse.modules.cronograma.finder.BlocoHorarioTemplateFinder;
import com.kyofoundation.skillnapse.modules.cronograma.finder.ExcecaoDiariaFinder;
import com.kyofoundation.skillnapse.modules.cronograma.finder.TemplateSemanalFinder;
import com.kyofoundation.skillnapse.modules.cronograma.mapper.ExcecaoDiariaMapper;
import com.kyofoundation.skillnapse.modules.cronograma.repository.ExcecaoDiariaRepository;
import com.kyofoundation.skillnapse.modules.cronograma.support.ProjecaoCronogramaSupport;
import com.kyofoundation.skillnapse.modules.cronograma.validator.ExcecaoDiariaValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CronogramaDiarioService {

    private final UserFinder userFinder;
    private final BlocoHorarioTemplateFinder blocoHorarioTemplateFinder;
    private final ExcecaoDiariaFinder excecaoDiariaFinder;
    private final MateriaFinder materiaFinder;
    private final TopicoFinder topicoFinder;
    private final UsuarioValidator usuarioValidator;
    private final ExcecaoDiariaValidator excecaoDiariaValidator;
    private final ProjecaoCronogramaSupport projecaoCronogramaSupport;
    private final ExcecaoDiariaRepository excecaoDiariaRepository;
    private final TemplateSemanalFinder templateSemanalFinder;

    @Transactional
    public ExcecaoDiariaResponse registrarExcecao(UUID userId, LocalDate data, RegistrarExcecaoDiariaRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        BlocoHorarioTemplate blocoOrigem = request.blocoTemplateOrigem() != null
                ? blocoHorarioTemplateFinder.findById(request.blocoTemplateOrigem())
                : null;
        excecaoDiariaValidator.validarPropriedadeBlocoTemplate(usuario, blocoOrigem);
        excecaoDiariaValidator.validarCriacao(request, data, usuario, blocoOrigem);

        Materia materia = request.materiaId() != null ? materiaFinder.findById(request.materiaId()) : null;
        Topico topico = request.topicoId() != null ? topicoFinder.findById(request.topicoId()) : null;
        excecaoDiariaValidator.validarMateriaETopico(usuario, materia, topico);

        ExcecaoDiaria excecaoDiaria = ExcecaoDiariaMapper.toCriarExcecao(usuario, blocoOrigem, data, request, materia, topico);
        excecaoDiariaRepository.save(excecaoDiaria);

        return ExcecaoDiariaMapper.toResponse(excecaoDiaria);
    }

    @Transactional
    public ExcecaoDiariaResponse editarExcecaoDiaria(UUID userId, UUID excecaoId, EditarExcecaoDiariaRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        ExcecaoDiaria excecaoDiaria = excecaoDiariaFinder.findById(excecaoId);
        excecaoDiariaValidator.validarPropriedade(usuario, excecaoDiaria);

        BlocoHorarioTemplate blocoOrigemNovo = request.blocoTemplateOrigem() != null
                ? blocoHorarioTemplateFinder.findById(request.blocoTemplateOrigem())
                : null;
        excecaoDiariaValidator.validarPropriedadeBlocoTemplate(usuario, blocoOrigemNovo);
        excecaoDiariaValidator.validarEdicao(request, excecaoDiaria, blocoOrigemNovo);

        Materia materiaNova = request.materiaId() != null ? materiaFinder.findById(request.materiaId()) : null;
        Topico topicoNovo = request.topicoId() != null ? topicoFinder.findById(request.topicoId()) : null;
        excecaoDiariaValidator.validarMateriaETopico(usuario, materiaNova, topicoNovo);

        ExcecaoDiariaMapper.toEditarExcecao(request, excecaoDiaria, blocoOrigemNovo, materiaNova, topicoNovo);
        excecaoDiariaRepository.save(excecaoDiaria);

        return ExcecaoDiariaMapper.toResponse(excecaoDiaria);
    }

    @Transactional
    public void removerExcecaoDiaria(UUID userId, UUID excecaoId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        ExcecaoDiaria excecaoDiaria = excecaoDiariaFinder.findById(excecaoId);
        excecaoDiariaValidator.validarPropriedade(usuario, excecaoDiaria);

        excecaoDiariaRepository.delete(excecaoDiaria);
    }

    @Transactional
    public void resetarAgendaDia(UUID userId, LocalDate data) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        excecaoDiariaRepository.deleteAllByUsuarioAndDataExcecao(usuario, data);
    }

    @Transactional(readOnly = true)
    public AgendaDiariaResponse visualizarAgendaDia(UUID userId, LocalDate data) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        DiaSemana diaSemana = DiaSemana.fromDayOfWeek(data.getDayOfWeek());

        Optional<TemplateSemanal> templateAtivoOpt = templateSemanalFinder.findAtivoOpcionalByUsuario(usuario);
        TemplateSemanal templateAtivo = templateAtivoOpt.orElse(null);

        List<BlocoHorarioTemplate> blocosTemplate = templateAtivo != null
                ? blocoHorarioTemplateFinder.findAllByTemplateSemanalAndDiaSemana(templateAtivo, diaSemana)
                : List.of();

        List<ExcecaoDiaria> excecoes = excecaoDiariaFinder.findAllByUsuarioEData(usuario, data);

        return projecaoCronogramaSupport.projetarAgendaDoDia(data, templateAtivo, blocosTemplate, excecoes);
    }
}
