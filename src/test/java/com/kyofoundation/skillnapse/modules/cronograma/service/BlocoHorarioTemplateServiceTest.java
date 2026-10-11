package com.kyofoundation.skillnapse.modules.cronograma.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.CriarBlocoHorarioRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.EditarBlocoHorarioRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.BlocoHorarioResponse;
import com.kyofoundation.skillnapse.modules.cronograma.entity.BlocoHorarioTemplate;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import com.kyofoundation.skillnapse.modules.cronograma.enums.DiaSemana;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import com.kyofoundation.skillnapse.modules.cronograma.finder.BlocoHorarioTemplateFinder;
import com.kyofoundation.skillnapse.modules.cronograma.finder.TemplateSemanalFinder;
import com.kyofoundation.skillnapse.modules.cronograma.repository.BlocoHorarioTemplateRepository;
import com.kyofoundation.skillnapse.modules.cronograma.validator.BlocoHorarioTemplateValidator;
import com.kyofoundation.skillnapse.modules.cronograma.validator.TemplateSemanalValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlocoHorarioTemplateServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private TemplateSemanalFinder templateSemanalFinder;

    @Mock
    private BlocoHorarioTemplateFinder blocoHorarioTemplateFinder;

    @Mock
    private MateriaFinder materiaFinder;

    @Mock
    private TopicoFinder topicoFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private TemplateSemanalValidator templateSemanalValidator;

    @Mock
    private BlocoHorarioTemplateValidator blocoHorarioTemplateValidator;

    @Mock
    private BlocoHorarioTemplateRepository blocoHorarioTemplateRepository;

    @InjectMocks
    private BlocoHorarioTemplateService blocoHorarioTemplateService;

    @Test
    @DisplayName("Deve adicionar bloco de horario no template com sucesso")
    void deveAdicionarBlocoComSucesso() {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        TemplateSemanal template = TemplateSemanal.builder().id(templateId).build();
        Materia materia = Materia.builder().id(materiaId).nome("Direito Penal").build();

        CriarBlocoHorarioRequest request = new CriarBlocoHorarioRequest(
                DiaSemana.SEGUNDA,
                LocalTime.of(14, 0),
                LocalTime.of(15, 30),
                TipoBloco.FOCO_TEORIA,
                materiaId,
                null
        );

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(templateSemanalFinder.findById(templateId)).thenReturn(template);
        when(materiaFinder.findById(materiaId)).thenReturn(materia);

        BlocoHorarioResponse response = blocoHorarioTemplateService.adicionarBlocoHorario(userId, templateId, request);

        assertThat(response.diaSemana()).isEqualTo(DiaSemana.SEGUNDA);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.materiaId()).isEqualTo(materiaId);
        verify(blocoHorarioTemplateRepository).save(any(BlocoHorarioTemplate.class));
    }

    @Test
    @DisplayName("Deve listar blocos de horario paginados")
    void deveListarBlocosPaginados() {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        TemplateSemanal template = TemplateSemanal.builder().id(templateId).build();
        Pageable pageable = PageRequest.of(0, 10);
        BlocoHorarioTemplate bloco = BlocoHorarioTemplate.builder()
                .id(UUID.randomUUID())
                .templateSemanal(template)
                .diaSemana(DiaSemana.TERCA)
                .horaInicio(LocalTime.of(10, 0))
                .horaFim(LocalTime.of(11, 0))
                .tipoBloco(TipoBloco.REVISAO)
                .build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(templateSemanalFinder.findById(templateId)).thenReturn(template);
        when(blocoHorarioTemplateFinder.findByTemplateSemanal(template, pageable))
                .thenReturn(new PageImpl<>(List.of(bloco)));

        Page<BlocoHorarioResponse> result = blocoHorarioTemplateService.listarBlocosHorario(userId, templateId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).diaSemana()).isEqualTo(DiaSemana.TERCA);
    }

    @Test
    @DisplayName("Deve visualizar bloco de horario especifico")
    void deveVisualizarBlocoEspecifico() {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID blocoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        TemplateSemanal template = TemplateSemanal.builder().id(templateId).build();
        BlocoHorarioTemplate bloco = BlocoHorarioTemplate.builder()
                .id(blocoId)
                .templateSemanal(template)
                .diaSemana(DiaSemana.QUARTA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(9, 30))
                .tipoBloco(TipoBloco.FOCO_TEORIA)
                .build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(templateSemanalFinder.findById(templateId)).thenReturn(template);
        when(blocoHorarioTemplateFinder.findById(blocoId)).thenReturn(bloco);

        BlocoHorarioResponse response = blocoHorarioTemplateService.visualizarBlocoHorario(userId, templateId, blocoId);

        assertThat(response.id()).isEqualTo(blocoId);
        assertThat(response.diaSemana()).isEqualTo(DiaSemana.QUARTA);
    }

    @Test
    @DisplayName("Deve editar bloco de horario com sucesso")
    void deveEditarBlocoComSucesso() {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID blocoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        TemplateSemanal template = TemplateSemanal.builder().id(templateId).build();
        BlocoHorarioTemplate bloco = BlocoHorarioTemplate.builder()
                .id(blocoId)
                .templateSemanal(template)
                .diaSemana(DiaSemana.QUINTA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(9, 0))
                .tipoBloco(TipoBloco.FOCO_TEORIA)
                .build();

        EditarBlocoHorarioRequest request = new EditarBlocoHorarioRequest(
                DiaSemana.SEXTA,
                LocalTime.of(15, 0),
                LocalTime.of(16, 30),
                TipoBloco.SIMULADO,
                null,
                null
        );

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(templateSemanalFinder.findById(templateId)).thenReturn(template);
        when(blocoHorarioTemplateFinder.findById(blocoId)).thenReturn(bloco);

        BlocoHorarioResponse response = blocoHorarioTemplateService.editarBlocoHorario(userId, templateId, blocoId, request);

        assertThat(response.diaSemana()).isEqualTo(DiaSemana.SEXTA);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(15, 0));
        assertThat(response.tipoBloco()).isEqualTo(TipoBloco.SIMULADO);
        verify(blocoHorarioTemplateRepository).save(bloco);
    }

    @Test
    @DisplayName("Deve resetar topico ao trocar materia sem informar novo topico")
    void deveResetarTopicoAoTrocarMateriaSemInformarNovoTopico() {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID blocoId = UUID.randomUUID();
        UUID materiaAntigaId = UUID.randomUUID();
        UUID materiaNovaId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        TemplateSemanal template = TemplateSemanal.builder().id(templateId).build();
        Materia materiaAntiga = Materia.builder().id(materiaAntigaId).nome("Direito Penal").build();
        Topico topicoAntigo = Topico.builder().id(UUID.randomUUID()).titulo("Crime").materia(materiaAntiga).build();
        Materia materiaNova = Materia.builder().id(materiaNovaId).nome("Direito Civil").build();

        BlocoHorarioTemplate bloco = BlocoHorarioTemplate.builder()
                .id(blocoId)
                .templateSemanal(template)
                .diaSemana(DiaSemana.SEGUNDA)
                .horaInicio(LocalTime.of(14, 0))
                .horaFim(LocalTime.of(16, 0))
                .materia(materiaAntiga)
                .topico(topicoAntigo)
                .build();

        EditarBlocoHorarioRequest request = new EditarBlocoHorarioRequest(
                DiaSemana.SEGUNDA,
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                TipoBloco.FOCO_TEORIA,
                materiaNovaId,
                null,
                false,
                false
        );

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(templateSemanalFinder.findById(templateId)).thenReturn(template);
        when(blocoHorarioTemplateFinder.findById(blocoId)).thenReturn(bloco);
        when(materiaFinder.findById(materiaNovaId)).thenReturn(materiaNova);

        BlocoHorarioResponse response = blocoHorarioTemplateService.editarBlocoHorario(userId, templateId, blocoId, request);

        assertThat(response.materiaId()).isEqualTo(materiaNovaId);
        assertThat(response.topicoId()).isNull();
        assertThat(bloco.getTopico()).isNull();
    }

    @Test
    @DisplayName("Deve desvincular materia e topico com sucesso")
    void deveDesvincularMateriaETopicoComSucesso() {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID blocoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        TemplateSemanal template = TemplateSemanal.builder().id(templateId).build();
        Materia materia = Materia.builder().id(UUID.randomUUID()).nome("História").build();

        BlocoHorarioTemplate bloco = BlocoHorarioTemplate.builder()
                .id(blocoId)
                .templateSemanal(template)
                .diaSemana(DiaSemana.SEGUNDA)
                .horaInicio(LocalTime.of(14, 0))
                .horaFim(LocalTime.of(16, 0))
                .materia(materia)
                .build();

        EditarBlocoHorarioRequest request = new EditarBlocoHorarioRequest(
                DiaSemana.SEGUNDA,
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                TipoBloco.FOCO_TEORIA,
                null,
                null,
                true,
                true
        );

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(templateSemanalFinder.findById(templateId)).thenReturn(template);
        when(blocoHorarioTemplateFinder.findById(blocoId)).thenReturn(bloco);

        BlocoHorarioResponse response = blocoHorarioTemplateService.editarBlocoHorario(userId, templateId, blocoId, request);

        assertThat(response.materiaId()).isNull();
        assertThat(response.topicoId()).isNull();
        assertThat(bloco.getMateria()).isNull();
        assertThat(bloco.getTopico()).isNull();
    }

    @Test
    @DisplayName("Deve apagar bloco de horario com sucesso")
    void deveApagarBlocoComSucesso() {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID blocoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        TemplateSemanal template = TemplateSemanal.builder().id(templateId).build();
        BlocoHorarioTemplate bloco = BlocoHorarioTemplate.builder()
                .id(blocoId)
                .templateSemanal(template)
                .build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(templateSemanalFinder.findById(templateId)).thenReturn(template);
        when(blocoHorarioTemplateFinder.findById(blocoId)).thenReturn(bloco);

        blocoHorarioTemplateService.apagarBlocoHorario(userId, templateId, blocoId);

        verify(blocoHorarioTemplateRepository).delete(bloco);
    }
}
