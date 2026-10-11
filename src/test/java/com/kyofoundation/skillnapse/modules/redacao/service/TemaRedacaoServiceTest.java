package com.kyofoundation.skillnapse.modules.redacao.service;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.desempenho.validator.DesempenhoValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.redacao.dto.payload.TemaRedacaoIaPayload;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.GerarTemaRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResumoResponse;
import com.kyofoundation.skillnapse.modules.redacao.entity.TemaRedacao;
import com.kyofoundation.skillnapse.modules.redacao.finder.TemaRedacaoFinder;
import com.kyofoundation.skillnapse.modules.redacao.repository.TemaRedacaoRepository;
import com.kyofoundation.skillnapse.modules.redacao.support.PromptTemaRedacaoSupport;
import com.kyofoundation.skillnapse.modules.redacao.validator.TemaRedacaoValidator;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemaRedacaoServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private PlanoEstudoFinder planoEstudoFinder;

    @Mock
    private DesempenhoValidator desempenhoValidator;

    @Mock
    private MateriaFinder materiaFinder;

    @Mock
    private TopicoFinder topicoFinder;

    @Mock
    private TemaRedacaoValidator temaRedacaoValidator;

    @Mock
    private TemaRedacaoFinder temaRedacaoFinder;

    @Mock
    private TemaRedacaoRepository temaRedacaoRepository;

    @Mock
    private PromptTemaRedacaoSupport promptTemaRedacaoSupport;

    @Mock
    private AiOrchestratorService aiOrchestratorService;

    @InjectMocks
    private TemaRedacaoService temaRedacaoService;

    private Usuario usuario;
    private PlanoEstudo plano;
    private Materia materia;
    private Topico topico;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Concurseiro Alfa")
                .ativo(true)
                .build();

        plano = PlanoEstudo.builder()
                .id(UUID.randomUUID())
                .titulo("Plano TCU")
                .usuario(usuario)
                .build();

        materia = Materia.builder()
                .id(UUID.randomUUID())
                .nome("Direito Financeiro")
                .planoEstudo(plano)
                .build();

        topico = Topico.builder()
                .id(UUID.randomUUID())
                .titulo("Lei de Responsabilidade Fiscal")
                .materia(materia)
                .build();
    }

    @Test
    @DisplayName("Deve gerar tema de redação com todos os parâmetros com sucesso")
    void deveGerarTemaComSucesso() {
        GerarTemaRedacaoRequest request = new GerarTemaRedacaoRequest(
                plano.getId(), materia.getId(), topico.getId(), "Cebraspe", "Estudo de Caso", null
        );

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(planoEstudoFinder.findById(plano.getId())).thenReturn(plano);
        when(materiaFinder.findById(materia.getId())).thenReturn(materia);
        when(topicoFinder.findById(topico.getId())).thenReturn(topico);

        when(promptTemaRedacaoSupport.construirPromptGeracao(
                eq(plano), eq(materia), eq(topico), eq("Cebraspe"), eq("Estudo de Caso"), isNull()
        )).thenReturn("Prompt de Redação Formatado");

        TemaRedacaoIaPayload payload = new TemaRedacaoIaPayload(
                "O Equilíbrio Fiscal e a Responsabilidade na Gestão Pública",
                "Contextualização sobre limites fiscais.",
                "Texto I: A LRF impõe limites de despesas. Texto II: Sanções por descumprimento.",
                "Redija até 30 linhas abordando os limites de despesa de pessoal e mecanismos sancionatórios."
        );
        when(aiOrchestratorService.generateStructured(any(PromptRequest.class), eq(TemaRedacaoIaPayload.class)))
                .thenReturn(payload);

        UUID temaId = UUID.randomUUID();
        when(temaRedacaoRepository.save(any(TemaRedacao.class))).thenAnswer(invocation -> {
            TemaRedacao t = invocation.getArgument(0);
            t.setId(temaId);
            t.setCriadoEm(Instant.now());
            return t;
        });

        TemaRedacaoResponse response = temaRedacaoService.gerarTema(usuario.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(temaId);
        assertThat(response.titulo()).isEqualTo("O Equilíbrio Fiscal e a Responsabilidade na Gestão Pública");
        assertThat(response.planoEstudoId()).isEqualTo(plano.getId());
        assertThat(response.geradoPorIa()).isTrue();
        assertThat(response.totalSubmissoes()).isEqualTo(0L);

        verify(temaRedacaoValidator).validarRequisicao(request);
        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(desempenhoValidator).validarPropriedadePlano(usuario, plano);
        verify(temaRedacaoValidator).validarMateriaPertencePlano(materia, plano);
        verify(temaRedacaoValidator).validarTopicoPertencePlano(topico, plano);
        verify(temaRedacaoValidator).validarTopicoPertenceMateria(topico, materia);
        verify(temaRedacaoValidator).validarTemaGeradoIa(payload);
        verify(temaRedacaoRepository).save(any(TemaRedacao.class));
    }

    @Test
    @DisplayName("Deve listar temas de redação por plano de estudo paginados")
    void deveListarTemasPorPlano() {
        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(planoEstudoFinder.findById(plano.getId())).thenReturn(plano);

        TemaRedacao tema1 = TemaRedacao.builder()
                .id(UUID.randomUUID())
                .planoEstudo(plano)
                .titulo("Tema 1")
                .geradoPorIa(true)
                .criadoEm(Instant.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<TemaRedacao> page = new PageImpl<>(List.of(tema1), pageable, 1);
        when(temaRedacaoRepository.findByPlanoEstudoOrderByCriadoEmDesc(plano, pageable)).thenReturn(page);
        when(temaRedacaoRepository.countSubmissoesByTemaId(tema1.getId())).thenReturn(2L);

        Page<TemaRedacaoResumoResponse> resultado = temaRedacaoService.listarPorPlano(usuario.getId(), plano.getId(), pageable);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst().titulo()).isEqualTo("Tema 1");
        assertThat(resultado.getContent().getFirst().totalSubmissoes()).isEqualTo(2L);

        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(desempenhoValidator).validarPropriedadePlano(usuario, plano);
    }

    @Test
    @DisplayName("Deve obter tema de redação detalhado por ID")
    void deveObterTemaPorId() {
        UUID temaId = UUID.randomUUID();
        TemaRedacao tema = TemaRedacao.builder()
                .id(temaId)
                .planoEstudo(plano)
                .titulo("Tema Detalhado")
                .textosMotivadores("Excertos e artigos de lei.")
                .criteriosAvaliacao("Diretrizes formais.")
                .geradoPorIa(true)
                .criadoEm(Instant.now())
                .build();

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(temaRedacaoFinder.findById(temaId)).thenReturn(tema);
        when(temaRedacaoRepository.countSubmissoesByTemaId(temaId)).thenReturn(5L);

        TemaRedacaoResponse response = temaRedacaoService.obterPorId(usuario.getId(), temaId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(temaId);
        assertThat(response.titulo()).isEqualTo("Tema Detalhado");
        assertThat(response.totalSubmissoes()).isEqualTo(5L);

        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(desempenhoValidator).validarPropriedadePlano(usuario, plano);
    }
}
