package com.kyofoundation.skillnapse.modules.questao.service;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.TopicoCriticoResponse;
import com.kyofoundation.skillnapse.modules.desempenho.enums.NivelCriticidadeTopico;
import com.kyofoundation.skillnapse.modules.desempenho.service.DesempenhoService;
import com.kyofoundation.skillnapse.modules.desempenho.validator.DesempenhoValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.MateriaRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.AlternativaGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.LoteQuestoesIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.QuestaoGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.request.GerarSimuladoAdaptativoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.SimuladoAdaptativoResponse;
import com.kyofoundation.skillnapse.modules.questao.entity.Questao;
import com.kyofoundation.skillnapse.modules.questao.entity.Simulado;
import com.kyofoundation.skillnapse.modules.questao.enums.TipoSimulado;
import com.kyofoundation.skillnapse.modules.questao.repository.QuestaoRepository;
import com.kyofoundation.skillnapse.modules.questao.repository.SimuladoRepository;
import com.kyofoundation.skillnapse.modules.questao.support.HashEnunciadoSupport;
import com.kyofoundation.skillnapse.modules.questao.support.PromptQuestaoAdaptativaSupport;
import com.kyofoundation.skillnapse.modules.questao.validator.SimuladoAdaptativoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimuladoAdaptativoServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private PlanoEstudoFinder planoEstudoFinder;

    @Mock
    private DesempenhoValidator desempenhoValidator;

    @Mock
    private SimuladoAdaptativoValidator simuladoAdaptativoValidator;

    @Mock
    private TopicoFinder topicoFinder;

    @Mock
    private TopicoRepository topicoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private DesempenhoService desempenhoService;

    @Mock
    private AiOrchestratorService aiOrchestratorService;

    @Mock
    private PromptQuestaoAdaptativaSupport promptQuestaoAdaptativaSupport;

    @Mock
    private HashEnunciadoSupport hashEnunciadoSupport;

    @Mock
    private QuestaoRepository questaoRepository;

    @Mock
    private SimuladoRepository simuladoRepository;

    @InjectMocks
    private SimuladoAdaptativoService simuladoAdaptativoService;

    private Usuario usuario;
    private PlanoEstudo plano;
    private Materia materia;
    private Topico topico1;
    private Topico topico2;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Concurseiro Focado")
                .email("concurseiro@skillnapse.com")
                .ativo(true)
                .build();

        plano = PlanoEstudo.builder()
                .id(UUID.randomUUID())
                .titulo("Plano TCU")
                .usuario(usuario)
                .build();

        materia = Materia.builder()
                .id(UUID.randomUUID())
                .nome("Controle Externo")
                .planoEstudo(plano)
                .build();

        topico1 = Topico.builder()
                .id(UUID.randomUUID())
                .titulo("Competências Constitucionais")
                .materia(materia)
                .build();

        topico2 = Topico.builder()
                .id(UUID.randomUUID())
                .titulo("Tomada de Contas Especial")
                .materia(materia)
                .build();
    }

    @Test
    @DisplayName("Deve gerar simulado adaptativo com tópicos informados manualmente")
    void deveGerarSimuladoComTopicosManuais() {
        UUID topicoId = topico1.getId();
        GerarSimuladoAdaptativoRequest request = new GerarSimuladoAdaptativoRequest(
                plano.getId(), 3, "FGV", List.of(topicoId)
        );

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(planoEstudoFinder.findById(plano.getId())).thenReturn(plano);
        when(topicoFinder.findById(topicoId)).thenReturn(topico1);

        UUID simuladoId = UUID.randomUUID();
        when(simuladoRepository.save(any(Simulado.class))).thenAnswer(invocation -> {
            Simulado s = invocation.getArgument(0);
            s.setId(simuladoId);
            return s;
        });

        when(promptQuestaoAdaptativaSupport.construirPromptGeracao(eq(topico1), eq(3), eq("FGV")))
                .thenReturn("Prompt para FGV");

        LoteQuestoesIaPayload lote = new LoteQuestoesIaPayload(List.of(
                criarQuestaoPayload("Questão 1 sobre Competências"),
                criarQuestaoPayload("Questão 2 sobre Competências"),
                criarQuestaoPayload("Questão 3 sobre Competências")
        ));
        when(aiOrchestratorService.generateStructured(any(PromptRequest.class), eq(LoteQuestoesIaPayload.class)))
                .thenReturn(lote);

        when(hashEnunciadoSupport.gerarHash(anyString())).thenReturn("hash-123");
        when(questaoRepository.save(any(Questao.class))).thenAnswer(invocation -> {
            Questao q = invocation.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        SimuladoAdaptativoResponse response = simuladoAdaptativoService.gerarSimuladoAdaptativo(usuario.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.simuladoId()).isEqualTo(simuladoId);
        assertThat(response.tipo()).isEqualTo(TipoSimulado.ADAPTATIVO_IA);
        assertThat(response.titulo()).contains("FGV");
        assertThat(response.totalQuestoes()).isEqualTo(3);
        assertThat(response.questoes()).hasSize(3);

        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(simuladoAdaptativoValidator).validarRequisicao(request);
        verify(desempenhoValidator).validarPropriedadePlano(usuario, plano);
        verify(simuladoAdaptativoValidator).validarTopicosPertencemPlano(anyList(), eq(plano));
        verify(simuladoAdaptativoValidator).validarQuestoesGeradasIa(lote.questoes());
        verify(questaoRepository, times(3)).save(any(Questao.class));
    }

    @Test
    @DisplayName("Deve gerar simulado adaptativo focando nos tópicos críticos quando não informados manualmente")
    void deveGerarSimuladoComTopicosCriticosAutomaticos() {
        GerarSimuladoAdaptativoRequest request = new GerarSimuladoAdaptativoRequest(
                plano.getId(), 4, "Cebraspe", null
        );

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(planoEstudoFinder.findById(plano.getId())).thenReturn(plano);

        List<TopicoCriticoResponse> criticos = List.of(
                new TopicoCriticoResponse(topico1.getId(), topico1.getTitulo(), materia.getId(), materia.getNome(), 3, 20.0, 10L, 80.0, NivelCriticidadeTopico.CRITICO),
                new TopicoCriticoResponse(topico2.getId(), topico2.getTitulo(), materia.getId(), materia.getNome(), 2, 40.0, 5L, 60.0, NivelCriticidadeTopico.ATENCAO)
        );
        when(desempenhoService.obterTopicosCriticos(usuario.getId(), plano.getId(), 4))
                .thenReturn(criticos);
        when(topicoFinder.findById(topico1.getId())).thenReturn(topico1);
        when(topicoFinder.findById(topico2.getId())).thenReturn(topico2);

        when(simuladoRepository.save(any(Simulado.class))).thenAnswer(invocation -> {
            Simulado s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        when(promptQuestaoAdaptativaSupport.construirPromptGeracao(any(Topico.class), anyInt(), any()))
                .thenReturn("Prompt adaptativo crítico");

        LoteQuestoesIaPayload lote = new LoteQuestoesIaPayload(List.of(
                criarQuestaoPayload("Questão A"),
                criarQuestaoPayload("Questão B")
        ));
        when(aiOrchestratorService.generateStructured(any(PromptRequest.class), eq(LoteQuestoesIaPayload.class)))
                .thenReturn(lote);

        when(hashEnunciadoSupport.gerarHash(anyString())).thenReturn("hash-critico");
        when(questaoRepository.save(any(Questao.class))).thenAnswer(invocation -> {
            Questao q = invocation.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        SimuladoAdaptativoResponse response = simuladoAdaptativoService.gerarSimuladoAdaptativo(usuario.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.totalQuestoes()).isEqualTo(4);
        verify(desempenhoService).obterTopicosCriticos(usuario.getId(), plano.getId(), 4);
        verify(promptQuestaoAdaptativaSupport, atLeastOnce()).construirPromptGeracao(any(Topico.class), anyInt(), eq("Cebraspe"));
    }

    @Test
    @DisplayName("Deve recorrer aos tópicos do plano quando não houver histórico de desempenho e topicoIds for nulo")
    void deveRecorrerAosTopicosDoPlanoQuandoSemHistorico() {
        GerarSimuladoAdaptativoRequest request = new GerarSimuladoAdaptativoRequest(
                plano.getId(), 3, null, null
        );

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(planoEstudoFinder.findById(plano.getId())).thenReturn(plano);

        when(desempenhoService.obterTopicosCriticos(usuario.getId(), plano.getId(), 3))
                .thenReturn(Collections.emptyList());

        when(materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano)).thenReturn(List.of(materia));
        when(topicoRepository.findByMateriaIn(List.of(materia))).thenReturn(List.of(topico1));

        when(simuladoRepository.save(any(Simulado.class))).thenAnswer(invocation -> {
            Simulado s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        when(promptQuestaoAdaptativaSupport.construirPromptGeracao(any(Topico.class), anyInt(), any()))
                .thenReturn("Prompt plano fallback");

        LoteQuestoesIaPayload lote = new LoteQuestoesIaPayload(List.of(
                criarQuestaoPayload("Questão sem histórico 1"),
                criarQuestaoPayload("Questão sem histórico 2"),
                criarQuestaoPayload("Questão sem histórico 3")
        ));
        when(aiOrchestratorService.generateStructured(any(PromptRequest.class), eq(LoteQuestoesIaPayload.class)))
                .thenReturn(lote);

        when(hashEnunciadoSupport.gerarHash(anyString())).thenReturn("hash-sem-hist");
        when(questaoRepository.save(any(Questao.class))).thenAnswer(invocation -> {
            Questao q = invocation.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        SimuladoAdaptativoResponse response = simuladoAdaptativoService.gerarSimuladoAdaptativo(usuario.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.titulo()).isEqualTo("Simulado: " + materia.getNome() + " - " + topico1.getTitulo());
        assertThat(response.questoes()).hasSize(3);

        verify(materiaRepository).findByPlanoEstudoOrderByOrdemAsc(plano);
        verify(topicoRepository).findByMateriaIn(List.of(materia));
    }

    private QuestaoGeradaIaPayload criarQuestaoPayload(String enunciado) {
        return new QuestaoGeradaIaPayload(
                enunciado,
                "Justificativa da resposta",
                "MEDIA",
                List.of(
                        new AlternativaGeradaIaPayload("Opção A", false, 1),
                        new AlternativaGeradaIaPayload("Opção B", true, 2),
                        new AlternativaGeradaIaPayload("Opção C", false, 3),
                        new AlternativaGeradaIaPayload("Opção D", false, 4)
                )
        );
    }
}
