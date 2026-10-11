package com.kyofoundation.skillnapse.modules.edital.service;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.edital.dto.request.AtualizarRascunhoEditalRequest;
import com.kyofoundation.skillnapse.modules.edital.dto.request.ConverterRascunhoEditalRequest;
import com.kyofoundation.skillnapse.modules.edital.dto.response.ConversaoEditalResponse;
import com.kyofoundation.skillnapse.modules.edital.dto.response.RascunhoEditalResponse;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalArvoreEstruturada;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalCargosDetectados;
import com.kyofoundation.skillnapse.modules.edital.entity.RascunhoEdital;
import com.kyofoundation.skillnapse.modules.edital.enums.StatusRascunhoEdital;
import com.kyofoundation.skillnapse.modules.edital.finder.RascunhoEditalFinder;
import com.kyofoundation.skillnapse.modules.edital.repository.RascunhoEditalRepository;
import com.kyofoundation.skillnapse.modules.edital.support.EditalCargoSupport;
import com.kyofoundation.skillnapse.modules.edital.support.EditalConversaoSupport;
import com.kyofoundation.skillnapse.modules.edital.support.EditalPromptSupport;
import com.kyofoundation.skillnapse.modules.edital.support.PdfTextExtractorSupport;
import com.kyofoundation.skillnapse.modules.edital.validator.EditalUploadValidator;
import com.kyofoundation.skillnapse.modules.edital.validator.RascunhoEditalValidator;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditalParserServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private RascunhoEditalFinder rascunhoEditalFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private EditalUploadValidator editalUploadValidator;

    @Mock
    private RascunhoEditalValidator rascunhoEditalValidator;

    @Mock
    private PdfTextExtractorSupport pdfTextExtractorSupport;

    @Mock
    private EditalPromptSupport editalPromptSupport;

    @Mock
    private EditalConversaoSupport editalConversaoSupport;

    @Mock
    private EditalCargoSupport editalCargoSupport;

    @Mock
    private AiOrchestratorService aiOrchestratorService;

    @Mock
    private RascunhoEditalRepository rascunhoEditalRepository;

    @InjectMocks
    private EditalParserService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(UUID.randomUUID()).email("aluno@skillnapse.com").build();
    }

    @Test
    @DisplayName("Deve detectar cargos disponíveis no edital via IA com sucesso")
    void deveDetectarCargosComSucesso() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "edital_tjpe.pdf", "application/pdf", "bytes".getBytes());
        PromptRequest promptCargos = new PromptRequest("detectar", "sys");
        EditalCargosDetectados detectados = new EditalCargosDetectados("TJPE 2026", List.of("Técnico Judiciário", "Analista Judiciário"));

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(pdfTextExtractorSupport.extrairTexto(arquivo)).thenReturn("Texto do edital TJPE");
        when(editalPromptSupport.criarPromptParaDeteccaoCargos("Texto do edital TJPE")).thenReturn(promptCargos);
        when(aiOrchestratorService.generateStructured(promptCargos, EditalCargosDetectados.class)).thenReturn(detectados);
        when(editalCargoSupport.enriquecerCargosSeNecessario("Texto do edital TJPE", detectados.cargos()))
                .thenReturn(detectados.cargos());

        EditalCargosDetectados resultado = service.detectarCargos(usuario.getId(), arquivo);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nomeConcurso()).isEqualTo("TJPE 2026");
        assertThat(resultado.cargos()).containsExactly("Técnico Judiciário", "Analista Judiciário");

        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(editalUploadValidator).validarArquivo(arquivo);
    }

    @Test
    @DisplayName("Deve fazer upload de edital e gerar rascunho com sucesso")
    void deveFazerUploadDeEditalComSucesso() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "edital.pdf", "application/pdf", "bytes".getBytes());
        EditalArvoreEstruturada arvore = new EditalArvoreEstruturada("Concurso BB", "Escriturário", List.of());
        PromptRequest promptCargos = new PromptRequest("cargos", "sys");
        PromptRequest promptRequest = new PromptRequest("user", "sys");
        EditalCargosDetectados cargosDetectados = new EditalCargosDetectados("Concurso BB", List.of("Escriturário"));

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(pdfTextExtractorSupport.extrairTexto(arquivo)).thenReturn("Texto extraido");
        when(editalPromptSupport.criarPromptParaDeteccaoCargos("Texto extraido")).thenReturn(promptCargos);
        when(aiOrchestratorService.generateStructured(promptCargos, EditalCargosDetectados.class)).thenReturn(cargosDetectados);
        when(editalCargoSupport.enriquecerCargosSeNecessario("Texto extraido", cargosDetectados.cargos()))
                .thenReturn(cargosDetectados.cargos());
        when(editalPromptSupport.criarPromptParaExtracao("Texto extraido", "Escriturário")).thenReturn(promptRequest);
        when(aiOrchestratorService.generateStructured(promptRequest, EditalArvoreEstruturada.class)).thenReturn(arvore);
        when(rascunhoEditalRepository.save(any(RascunhoEdital.class))).thenAnswer(i -> {
            RascunhoEdital r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RascunhoEditalResponse response = service.uploadEditalPdf(usuario.getId(), arquivo);

        assertThat(response).isNotNull();
        assertThat(response.conteudo().nomeConcurso()).isEqualTo("Concurso BB");

        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(editalUploadValidator).validarArquivo(arquivo);
        verify(rascunhoEditalRepository).save(any(RascunhoEdital.class));
    }

    @Test
    @DisplayName("Deve retornar opções de cargos quando IA identificar múltiplos cargos e usuário não informar cargoAlvo")
    void deveRetornarOpcoesDeCargoQuandoIaIdentificarMultiplosCargos() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "edital_pmpe.pdf", "application/pdf", "bytes".getBytes());
        PromptRequest promptCargos = new PromptRequest("detectar cargos", "sys");
        EditalCargosDetectados detectados = new EditalCargosDetectados("PMPE 2024", List.of("Soldado PM", "Oficial PM"));

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(pdfTextExtractorSupport.extrairTexto(arquivo)).thenReturn("Texto com múltiplos cargos");
        when(editalPromptSupport.criarPromptParaDeteccaoCargos("Texto com múltiplos cargos")).thenReturn(promptCargos);
        when(aiOrchestratorService.generateStructured(promptCargos, EditalCargosDetectados.class)).thenReturn(detectados);
        when(editalCargoSupport.enriquecerCargosSeNecessario("Texto com múltiplos cargos", detectados.cargos()))
                .thenReturn(detectados.cargos());

        RascunhoEditalResponse response = service.uploadEditalPdf(usuario.getId(), arquivo, null);

        assertThat(response).isNotNull();
        assertThat(response.precisaSelecionarCargo()).isTrue();
        assertThat(response.cargosDisponiveis()).containsExactly("Soldado PM", "Oficial PM");
        assertThat(response.conteudo().nomeConcurso()).isEqualTo("PMPE 2024");
        verify(rascunhoEditalRepository, never()).save(any(RascunhoEdital.class));
    }

    @Test
    @DisplayName("Deve gerar árvore direcionada diretamente quando cargoAlvo for informado")
    void deveGerarArvoreDirecionadaQuandoCargoAlvoForInformado() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "edital_pmpe.pdf", "application/pdf", "bytes".getBytes());
        EditalArvoreEstruturada arvore = new EditalArvoreEstruturada("PMPE 2024", "Soldado PM", List.of());
        PromptRequest promptRequest = new PromptRequest("extrair soldado", "sys");

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(pdfTextExtractorSupport.extrairTexto(arquivo)).thenReturn("Texto extraido");
        when(editalPromptSupport.criarPromptParaExtracao("Texto extraido", "Soldado PM")).thenReturn(promptRequest);
        when(aiOrchestratorService.generateStructured(promptRequest, EditalArvoreEstruturada.class)).thenReturn(arvore);
        when(rascunhoEditalRepository.save(any(RascunhoEdital.class))).thenAnswer(i -> {
            RascunhoEdital r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RascunhoEditalResponse response = service.uploadEditalPdf(usuario.getId(), arquivo, "Soldado PM");

        assertThat(response).isNotNull();
        assertThat(response.precisaSelecionarCargo()).isFalse();
        assertThat(response.conteudo().cargo()).isEqualTo("Soldado PM");
        verify(editalPromptSupport, never()).criarPromptParaDeteccaoCargos(any());
        verify(rascunhoEditalRepository).save(any(RascunhoEdital.class));
    }

    @Test
    @DisplayName("Deve listar rascunhos do usuario")
    void deveListarRascunhos() {
        Pageable pageable = PageRequest.of(0, 10);
        RascunhoEdital rascunho = RascunhoEdital.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .conteudoExtraidoJson("{\"nomeConcurso\":\"BB\",\"cargo\":\"Agente\",\"materias\":[]}")
                .build();

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(rascunhoEditalRepository.findAllByUsuarioId(usuario.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(rascunho)));

        Page<RascunhoEditalResponse> pagina = service.listarRascunhos(usuario.getId(), pageable);

        assertThat(pagina).isNotEmpty();
        assertThat(pagina.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Deve visualizar rascunho por id")
    void deveVisualizarRascunho() {
        UUID rascunhoId = UUID.randomUUID();
        RascunhoEdital rascunho = RascunhoEdital.builder()
                .id(rascunhoId)
                .usuario(usuario)
                .conteudoExtraidoJson("{\"nomeConcurso\":\"BB\",\"cargo\":\"Agente\",\"materias\":[]}")
                .build();

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(rascunhoEditalFinder.findById(rascunhoId)).thenReturn(rascunho);

        RascunhoEditalResponse response = service.visualizarRascunho(usuario.getId(), rascunhoId);

        assertThat(response).isNotNull();
        verify(rascunhoEditalValidator).validarPropriedade(usuario, rascunho);
    }

    @Test
    @DisplayName("Deve atualizar rascunho")
    void deveAtualizarRascunho() {
        UUID rascunhoId = UUID.randomUUID();
        RascunhoEdital rascunho = RascunhoEdital.builder()
                .id(rascunhoId)
                .usuario(usuario)
                .status(StatusRascunhoEdital.AGUARDANDO_APROVACAO)
                .build();

        EditalArvoreEstruturada novaArvore = new EditalArvoreEstruturada("Concurso Novo", "Cargo", List.of());
        AtualizarRascunhoEditalRequest request = new AtualizarRascunhoEditalRequest(novaArvore);

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(rascunhoEditalFinder.findById(rascunhoId)).thenReturn(rascunho);
        when(rascunhoEditalRepository.save(any(RascunhoEdital.class))).thenAnswer(i -> i.getArgument(0));

        RascunhoEditalResponse response = service.atualizarRascunho(usuario.getId(), rascunhoId, request);

        assertThat(response).isNotNull();
        verify(rascunhoEditalValidator).validarPropriedade(usuario, rascunho);
        verify(rascunhoEditalValidator).validarStatusParaEdicao(rascunho);
        verify(rascunhoEditalRepository).save(rascunho);
    }

    @Test
    @DisplayName("Deve converter rascunho em plano de estudo")
    void deveConverterRascunho() {
        UUID rascunhoId = UUID.randomUUID();
        RascunhoEdital rascunho = RascunhoEdital.builder()
                .id(rascunhoId)
                .usuario(usuario)
                .status(StatusRascunhoEdital.AGUARDANDO_APROVACAO)
                .build();

        ConverterRascunhoEditalRequest request = new ConverterRascunhoEditalRequest("Meu Plano", "Descricao");
        ConversaoEditalResponse respostaEsperada = new ConversaoEditalResponse(rascunhoId, UUID.randomUUID(), "Meu Plano", 2, 5);

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(rascunhoEditalFinder.findById(rascunhoId)).thenReturn(rascunho);
        when(editalConversaoSupport.executarConversao(usuario, rascunho, request)).thenReturn(respostaEsperada);

        ConversaoEditalResponse response = service.converterEmPlanoEstudo(usuario.getId(), rascunhoId, request);

        assertThat(response).isEqualTo(respostaEsperada);
        verify(rascunhoEditalValidator).validarPropriedade(usuario, rascunho);
        verify(rascunhoEditalValidator).validarStatusParaConversao(rascunho);
    }

    @Test
    @DisplayName("Deve excluir rascunho")
    void deveExcluirRascunho() {
        UUID rascunhoId = UUID.randomUUID();
        RascunhoEdital rascunho = RascunhoEdital.builder()
                .id(rascunhoId)
                .usuario(usuario)
                .build();

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(rascunhoEditalFinder.findById(rascunhoId)).thenReturn(rascunho);

        service.excluirRascunho(usuario.getId(), rascunhoId);

        verify(rascunhoEditalValidator).validarPropriedade(usuario, rascunho);
        verify(rascunhoEditalRepository).delete(rascunho);
    }
}
