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
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoAcaoExcecao;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import com.kyofoundation.skillnapse.modules.cronograma.finder.BlocoHorarioTemplateFinder;
import com.kyofoundation.skillnapse.modules.cronograma.finder.ExcecaoDiariaFinder;
import com.kyofoundation.skillnapse.modules.cronograma.finder.TemplateSemanalFinder;
import com.kyofoundation.skillnapse.modules.cronograma.repository.ExcecaoDiariaRepository;
import com.kyofoundation.skillnapse.modules.cronograma.support.ProjecaoCronogramaSupport;
import com.kyofoundation.skillnapse.modules.cronograma.validator.ExcecaoDiariaValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CronogramaDiarioServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private BlocoHorarioTemplateFinder blocoHorarioTemplateFinder;

    @Mock
    private ExcecaoDiariaFinder excecaoDiariaFinder;

    @Mock
    private MateriaFinder materiaFinder;

    @Mock
    private TopicoFinder topicoFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private ExcecaoDiariaValidator excecaoDiariaValidator;

    @Mock
    private ProjecaoCronogramaSupport projecaoCronogramaSupport;

    @Mock
    private ExcecaoDiariaRepository excecaoDiariaRepository;

    @Mock
    private TemplateSemanalFinder templateSemanalFinder;

    @InjectMocks
    private CronogramaDiarioService cronogramaDiarioService;

    @Test
    @DisplayName("Deve registrar excecao diaria com sucesso")
    void deveRegistrarExcecaoComSucesso() {
        UUID userId = UUID.randomUUID();
        LocalDate data = LocalDate.of(2026, 10, 15);
        Usuario usuario = Usuario.builder().id(userId).build();

        RegistrarExcecaoDiariaRequest request = new RegistrarExcecaoDiariaRequest(
                TipoAcaoExcecao.BLOCO_AVULSO,
                null,
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                TipoBloco.SIMULADO,
                null,
                null
        );

        when(userFinder.findById(userId)).thenReturn(usuario);

        ExcecaoDiariaResponse response = cronogramaDiarioService.registrarExcecao(userId, data, request);

        assertThat(response.dataExcecao()).isEqualTo(data);
        assertThat(response.tipoAcao()).isEqualTo(TipoAcaoExcecao.BLOCO_AVULSO);
        verify(excecaoDiariaRepository).save(any(ExcecaoDiaria.class));
    }

    @Test
    @DisplayName("Deve editar excecao diaria com sucesso")
    void deveEditarExcecaoComSucesso() {
        UUID userId = UUID.randomUUID();
        UUID excecaoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        ExcecaoDiaria excecao = ExcecaoDiaria.builder()
                .id(excecaoId)
                .usuario(usuario)
                .dataExcecao(LocalDate.of(2026, 10, 15))
                .tipoAcao(TipoAcaoExcecao.BLOCO_AVULSO)
                .horaInicio(LocalTime.of(18, 0))
                .horaFim(LocalTime.of(19, 0))
                .build();

        EditarExcecaoDiariaRequest request = new EditarExcecaoDiariaRequest(
                null,
                null,
                LocalTime.of(19, 0),
                LocalTime.of(20, 30),
                TipoBloco.REVISAO,
                null,
                null
        );

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(excecaoDiariaFinder.findById(excecaoId)).thenReturn(excecao);

        ExcecaoDiariaResponse response = cronogramaDiarioService.editarExcecaoDiaria(userId, excecaoId, request);

        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(19, 0));
        assertThat(response.horaFim()).isEqualTo(LocalTime.of(20, 30));
        verify(excecaoDiariaRepository).save(excecao);
    }

    @Test
    @DisplayName("Deve remover excecao diaria pontual")
    void deveRemoverExcecao() {
        UUID userId = UUID.randomUUID();
        UUID excecaoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).build();
        ExcecaoDiaria excecao = ExcecaoDiaria.builder().id(excecaoId).usuario(usuario).build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(excecaoDiariaFinder.findById(excecaoId)).thenReturn(excecao);

        cronogramaDiarioService.removerExcecaoDiaria(userId, excecaoId);

        verify(excecaoDiariaRepository).delete(excecao);
    }

    @Test
    @DisplayName("Deve resetar agenda do dia removendo todas as excecoes daquela data")
    void deveResetarAgendaDia() {
        UUID userId = UUID.randomUUID();
        LocalDate data = LocalDate.of(2026, 10, 15);
        Usuario usuario = Usuario.builder().id(userId).build();

        when(userFinder.findById(userId)).thenReturn(usuario);

        cronogramaDiarioService.resetarAgendaDia(userId, data);

        verify(excecaoDiariaRepository).deleteAllByUsuarioAndDataExcecao(usuario, data);
    }

    @Test
    @DisplayName("Deve visualizar agenda do dia delegando mesclagem para o suporte de projecao")
    void deveVisualizarAgendaDia() {
        UUID userId = UUID.randomUUID();
        LocalDate data = LocalDate.of(2026, 10, 15);
        Usuario usuario = Usuario.builder().id(userId).build();
        TemplateSemanal template = TemplateSemanal.builder().id(UUID.randomUUID()).nome("Padrão").build();
        AgendaDiariaResponse agendaEsperada = new AgendaDiariaResponse(data, DiaSemana.QUINTA, template.getId(), "Padrão", List.of());

        when(userFinder.findById(userId)).thenReturn(usuario);
        when(templateSemanalFinder.findAtivoOpcionalByUsuario(usuario)).thenReturn(Optional.of(template));
        when(blocoHorarioTemplateFinder.findAllByTemplateSemanalAndDiaSemana(eq(template), eq(DiaSemana.QUINTA)))
                .thenReturn(List.of());
        when(excecaoDiariaFinder.findAllByUsuarioEData(usuario, data)).thenReturn(List.of());
        when(projecaoCronogramaSupport.projetarAgendaDoDia(eq(data), eq(template), any(), any()))
                .thenReturn(agendaEsperada);

        AgendaDiariaResponse resultado = cronogramaDiarioService.visualizarAgendaDia(userId, data);

        assertThat(resultado).isEqualTo(agendaEsperada);
    }
}
