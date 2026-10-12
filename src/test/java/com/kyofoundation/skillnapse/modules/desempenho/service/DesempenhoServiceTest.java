package com.kyofoundation.skillnapse.modules.desempenho.service;

import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.PainelGeralDesempenhoResponse;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.RelatorioLacunasResponse;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.TopicoCriticoResponse;
import com.kyofoundation.skillnapse.modules.desempenho.enums.NivelCriticidadeTopico;
import com.kyofoundation.skillnapse.modules.desempenho.support.CalculoDesempenhoSupport;
import com.kyofoundation.skillnapse.modules.desempenho.validator.DesempenhoValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.MateriaRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.PlanoEstudoRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import com.kyofoundation.skillnapse.modules.questao.dto.projection.MetricasTopicoProjection;
import com.kyofoundation.skillnapse.modules.questao.finder.TentativaQuestaoFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DesempenhoServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private PlanoEstudoFinder planoEstudoFinder;

    @Mock
    private PlanoEstudoRepository planoEstudoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private TopicoRepository topicoRepository;

    @Mock
    private TentativaQuestaoFinder tentativaQuestaoFinder;

    @Mock
    private DesempenhoValidator desempenhoValidator;

    @Mock
    private CalculoDesempenhoSupport calculoDesempenhoSupport;

    @InjectMocks
    private DesempenhoService desempenhoService;

    @Test
    @DisplayName("Deve obter relatorio de lacunas com sucesso")
    void deveObterRelatorioLacunasComSucesso() {
        UUID userId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(true).build();
        PlanoEstudo plano = PlanoEstudo.builder().id(planoId).titulo("Concurso Fiscal").usuario(usuario).build();

        Materia materia = Materia.builder().id(UUID.randomUUID()).nome("Direito Tributario").corHex("#FF0000").planoEstudo(plano).build();
        Topico topico = Topico.builder().id(UUID.randomUUID()).titulo("Imunidades").materia(materia).pesoEdital(4).nivelProficiencia(NivelProficiencia.INICIANTE).build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        when(planoEstudoFinder.findById(planoId)).thenReturn(plano);
        doNothing().when(desempenhoValidator).validarPropriedadePlano(usuario, plano);

        when(materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano)).thenReturn(List.of(materia));
        when(topicoRepository.findByMateriaIn(List.of(materia))).thenReturn(List.of(topico));

        MetricasTopicoProjection metrica = mock(MetricasTopicoProjection.class);
        when(metrica.getTopicoId()).thenReturn(topico.getId());
        when(metrica.getTotalTentativas()).thenReturn(10L);
        when(metrica.getTotalAcertos()).thenReturn(4L);
        when(metrica.getTempoMedioSegundos()).thenReturn(45.0);

        when(tentativaQuestaoFinder.obterMetricasPorTopicos(any(), any())).thenReturn(List.of(metrica));
        when(calculoDesempenhoSupport.arredondar(anyDouble())).thenReturn(45.0);
        when(calculoDesempenhoSupport.calcularTaxaAcerto(10L, 4L)).thenReturn(40.0);
        when(calculoDesempenhoSupport.determinarCriticidade(10L, 40.0, 4, NivelProficiencia.INICIANTE))
                .thenReturn(NivelCriticidadeTopico.CRITICO);
        when(calculoDesempenhoSupport.calcularReadinessIndex(anyList())).thenReturn(40.0);

        RelatorioLacunasResponse response = desempenhoService.obterRelatorioLacunas(userId, planoId);

        assertThat(response).isNotNull();
        assertThat(response.planoTitulo()).isEqualTo("Concurso Fiscal");
        assertThat(response.totalTopicosCriticos()).isEqualTo(1);
        assertThat(response.materias()).hasSize(1);
        assertThat(response.materias().getFirst().topicos()).hasSize(1);
        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(desempenhoValidator).validarPropriedadePlano(usuario, plano);
    }

    @Test
    @DisplayName("Deve lancar ForbiddenException quando usuario inativo chamar relatorio de lacunas")
    void deveLancarForbiddenQuandoUsuarioInativo() {
        UUID userId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(false).build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        doThrow(new ForbiddenException("Usuário inativo ou bloqueado no sistema."))
                .when(usuarioValidator).validarUsuarioAtivo(usuario);

        assertThatThrownBy(() -> desempenhoService.obterRelatorioLacunas(userId, planoId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Usuário inativo ou bloqueado no sistema.");
    }

    @Test
    @DisplayName("Deve obter topicos criticos ordenados por severidade e limite")
    void deveObterTopicosCriticosOrdenados() {
        UUID userId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(true).build();
        PlanoEstudo plano = PlanoEstudo.builder().id(planoId).titulo("Plano Policial").usuario(usuario).build();

        Materia materia = Materia.builder().id(UUID.randomUUID()).nome("Penal").planoEstudo(plano).build();
        Topico topico1 = Topico.builder().id(UUID.randomUUID()).titulo("Crimes contra Pessoa").materia(materia).pesoEdital(5).nivelProficiencia(NivelProficiencia.INICIANTE).build();
        Topico topico2 = Topico.builder().id(UUID.randomUUID()).titulo("Crimes contra Patrimonio").materia(materia).pesoEdital(3).nivelProficiencia(NivelProficiencia.INTERMEDIARIO).build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        doNothing().when(desempenhoValidator).validarLimiteTopicosCriticos(5);
        when(planoEstudoFinder.findById(planoId)).thenReturn(plano);
        doNothing().when(desempenhoValidator).validarPropriedadePlano(usuario, plano);

        when(materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano)).thenReturn(List.of(materia));
        when(topicoRepository.findByMateriaIn(List.of(materia))).thenReturn(List.of(topico1, topico2));

        MetricasTopicoProjection metrica1 = mock(MetricasTopicoProjection.class);
        when(metrica1.getTopicoId()).thenReturn(topico1.getId());
        when(metrica1.getTotalTentativas()).thenReturn(10L);
        when(metrica1.getTotalAcertos()).thenReturn(3L);

        MetricasTopicoProjection metrica2 = mock(MetricasTopicoProjection.class);
        when(metrica2.getTopicoId()).thenReturn(topico2.getId());
        when(metrica2.getTotalTentativas()).thenReturn(8L);
        when(metrica2.getTotalAcertos()).thenReturn(4L);

        when(tentativaQuestaoFinder.obterMetricasPorTopicos(any(), any())).thenReturn(List.of(metrica1, metrica2));
        when(calculoDesempenhoSupport.calcularTaxaAcerto(10L, 3L)).thenReturn(30.0);
        when(calculoDesempenhoSupport.calcularTaxaAcerto(8L, 4L)).thenReturn(50.0);
        when(calculoDesempenhoSupport.determinarCriticidade(10L, 30.0, 5, NivelProficiencia.INICIANTE))
                .thenReturn(NivelCriticidadeTopico.CRITICO);
        when(calculoDesempenhoSupport.determinarCriticidade(8L, 50.0, 3, NivelProficiencia.INTERMEDIARIO))
                .thenReturn(NivelCriticidadeTopico.CRITICO);
        when(calculoDesempenhoSupport.calcularIndiceSeveridade(30.0, 5, 10L)).thenReturn(350.0);
        when(calculoDesempenhoSupport.calcularIndiceSeveridade(50.0, 3, 8L)).thenReturn(150.0);

        List<TopicoCriticoResponse> resultado = desempenhoService.obterTopicosCriticos(userId, planoId, 5);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.getFirst().tituloTopico()).isEqualTo("Crimes contra Pessoa");
        assertThat(resultado.getFirst().indiceSeveridade()).isEqualTo(350.0);
        assertThat(resultado.get(1).tituloTopico()).isEqualTo("Crimes contra Patrimonio");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando topicos nao possuem tentativas registradas")
    void deveRetornarListaVaziaQuandoTopicosSemTentativas() {
        UUID userId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(true).build();
        PlanoEstudo plano = PlanoEstudo.builder().id(planoId).titulo("Plano Policial").usuario(usuario).build();

        Materia materia = Materia.builder().id(UUID.randomUUID()).nome("Penal").planoEstudo(plano).build();
        Topico topico1 = Topico.builder().id(UUID.randomUUID()).titulo("Crimes contra Pessoa").materia(materia).pesoEdital(5).nivelProficiencia(NivelProficiencia.INICIANTE).build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        doNothing().when(desempenhoValidator).validarLimiteTopicosCriticos(5);
        when(planoEstudoFinder.findById(planoId)).thenReturn(plano);
        doNothing().when(desempenhoValidator).validarPropriedadePlano(usuario, plano);

        when(materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano)).thenReturn(List.of(materia));
        when(topicoRepository.findByMateriaIn(List.of(materia))).thenReturn(List.of(topico1));

        when(tentativaQuestaoFinder.obterMetricasPorTopicos(any(), any())).thenReturn(List.of());
        when(calculoDesempenhoSupport.calcularTaxaAcerto(0L, 0L)).thenReturn(0.0);
        when(calculoDesempenhoSupport.determinarCriticidade(0L, 0.0, 5, NivelProficiencia.INICIANTE))
                .thenReturn(NivelCriticidadeTopico.SEM_DADOS);
        when(calculoDesempenhoSupport.calcularIndiceSeveridade(0.0, 5, 0L)).thenReturn(0.0);

        List<TopicoCriticoResponse> resultado = desempenhoService.obterTopicosCriticos(userId, planoId, 5);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve obter painel geral consolidado com sucesso")
    void deveObterPainelGeralComSucesso() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(true).build();
        PlanoEstudo plano = PlanoEstudo.builder().id(UUID.randomUUID()).titulo("Geral").usuario(usuario).build();
        Materia materia = Materia.builder().id(UUID.randomUUID()).nome("TI").planoEstudo(plano).build();
        Topico topico = Topico.builder().id(UUID.randomUUID()).titulo("Bancos de Dados").materia(materia).pesoEdital(3).build();

        when(userFinder.findById(userId)).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        when(planoEstudoRepository.findAllByUsuario(usuario)).thenReturn(List.of(plano));
        when(materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano)).thenReturn(List.of(materia));
        when(topicoRepository.findByMateriaIn(anyList())).thenReturn(List.of(topico));

        MetricasTopicoProjection metrica = mock(MetricasTopicoProjection.class);
        when(metrica.getTopicoId()).thenReturn(topico.getId());
        when(metrica.getTotalTentativas()).thenReturn(20L);
        when(metrica.getTotalAcertos()).thenReturn(16L);
        when(metrica.getTempoMedioSegundos()).thenReturn(30.0);

        when(tentativaQuestaoFinder.obterTodasMetricasPorTopicosDoUsuario(usuario)).thenReturn(List.of(metrica));
        when(calculoDesempenhoSupport.arredondar(anyDouble())).thenReturn(30.0);
        when(calculoDesempenhoSupport.calcularTaxaAcerto(20L, 16L)).thenReturn(80.0);
        when(calculoDesempenhoSupport.determinarCriticidade(anyLong(), anyDouble(), any(), any()))
                .thenReturn(NivelCriticidadeTopico.ESTAVEL);
        when(calculoDesempenhoSupport.calcularIndiceSeveridade(anyDouble(), any(), anyLong())).thenReturn(60.0);

        PainelGeralDesempenhoResponse painel = desempenhoService.obterPainelGeral(userId);

        assertThat(painel).isNotNull();
        assertThat(painel.totalQuestoesRespondidas()).isEqualTo(20L);
        assertThat(painel.totalAcertos()).isEqualTo(16L);
        assertThat(painel.taxaAcertoGeral()).isEqualTo(80.0);
        assertThat(painel.materias()).hasSize(1);
    }
}
