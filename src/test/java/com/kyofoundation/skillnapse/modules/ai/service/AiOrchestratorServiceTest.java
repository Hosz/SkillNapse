package com.kyofoundation.skillnapse.modules.ai.service;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.ai.dto.AiGenerationResponse;
import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.validator.AiPromptValidator;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiOrchestratorServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatModel chatModel;

    @Mock
    private UserFinder userFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private com.kyofoundation.skillnapse.modules.ai.support.AiFallbackClientSupport aiFallbackClientSupport;

    private AiPromptValidator validator;
    private AiOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        validator = new AiPromptValidator();
        orchestratorService = new AiOrchestratorService(chatClientBuilder, chatModel, validator, userFinder, usuarioValidator, aiFallbackClientSupport);
    }

    record StructuredPlanResponse(String materia, List<String> topicos) {}

    @Test
    @DisplayName("Deve gerar resposta textual chamando diretamente a classe ChatClient")
    void deveGerarRespostaTextualComSucesso() {
        PromptRequest request = new PromptRequest("Explique o princípio SRP", "Você é um mentor");

        org.springframework.ai.chat.model.ChatResponse mockChatResponse = org.mockito.Mockito.mock(
                org.springframework.ai.chat.model.ChatResponse.class,
                Answers.RETURNS_DEEP_STUBS
        );
        when(mockChatResponse.getResult().getOutput().getText())
                .thenReturn("Single Responsibility Principle diz que uma classe deve ter apenas um motivo para mudar.");
        when(mockChatResponse.getMetadata().getModel()).thenReturn("gemini-2.5-flash");
        when(mockChatResponse.getMetadata().getUsage().getPromptTokens()).thenReturn(10);
        when(mockChatResponse.getMetadata().getUsage().getCompletionTokens()).thenReturn(20);
        when(mockChatResponse.getMetadata().getUsage().getTotalTokens()).thenReturn(30);

        when(chatClientBuilder.build().prompt().user(request.prompt()).system(request.systemMessage()).call().chatResponse())
                .thenReturn(mockChatResponse);

        AiGenerationResponse response = orchestratorService.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Single Responsibility Principle diz que uma classe deve ter apenas um motivo para mudar.");
        assertThat(response.model()).isEqualTo("gemini-2.5-flash");
        assertThat(response.promptTokens()).isEqualTo(10L);
        assertThat(response.generationTokens()).isEqualTo(20L);
        assertThat(response.totalTokens()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Deve validar usuário ativo ao chamar generate com userId")
    void deveValidarUsuarioAtivoComUserId() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(true).build();
        PromptRequest request = new PromptRequest("Prompt teste");

        when(userFinder.findById(userId)).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);

        org.springframework.ai.chat.model.ChatResponse mockChatResponse = org.mockito.Mockito.mock(
                org.springframework.ai.chat.model.ChatResponse.class,
                Answers.RETURNS_DEEP_STUBS
        );
        when(mockChatResponse.getResult().getOutput().getText()).thenReturn("Resposta teste");
        when(chatClientBuilder.build().prompt().user(request.prompt()).call().chatResponse()).thenReturn(mockChatResponse);

        AiGenerationResponse response = orchestratorService.generate(userId, request);

        assertThat(response).isNotNull();
        verify(usuarioValidator).validarUsuarioAtivo(usuario);
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando usuário inativo chamar generate")
    void deveLancarForbiddenComUsuarioInativo() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(false).build();
        PromptRequest request = new PromptRequest("Prompt teste");

        when(userFinder.findById(userId)).thenReturn(usuario);
        doThrow(new ForbiddenException("Usuário inativo ou bloqueado no sistema."))
                .when(usuarioValidator).validarUsuarioAtivo(usuario);

        assertThatThrownBy(() -> orchestratorService.generate(userId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Usuário inativo ou bloqueado no sistema.");
    }

    @Test
    @DisplayName("Deve lançar exceção BadRequestException quando prompt for nulo ou em branco")
    void deveLancarExcecaoQuandoPromptForVazio() {
        PromptRequest requestInvalido = new PromptRequest("   ");

        assertThatThrownBy(() -> orchestratorService.generate(requestInvalido))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O prompt não pode ser nulo ou vazio.");

        verify(chatClientBuilder, never()).build();
    }

    @Test
    @DisplayName("Deve limpar preâmbulo e blocos de código Markdown retornando estritamente JSON")
    void deveLimparPreambuloEJsonMarkdown() {
        String sujo = """
                Dos atos probatórios
                ```json
                {
                  "nomeConcurso": "TJPE",
                  "cargo": "Técnico",
                  "materias": []
                }
                ```
                Atenciosamente!
                """;

        String limpo = AiOrchestratorService.cleanJsonText(sujo);

        assertThat(limpo).startsWith("{").endsWith("}");
        assertThat(limpo).doesNotContain("Dos atos probatórios");
        assertThat(limpo).doesNotContain("```");
        assertThat(limpo).doesNotContain("Atenciosamente");
    }

    @Test
    @DisplayName("Deve limpar blocos de raciocínio de tags <think> e extrair JSON array")
    void deveLimparTagsDePensamentoEExtrairJsonArray() {
        String entradaComRaciocinio = """
                <think>
                Analisando o edital e extraindo tópicos...
                </think>
                ```
                [{"id": 1, "nome": "Português"}]
                ```
                """;

        String limpo = AiOrchestratorService.cleanJsonText(entradaComRaciocinio);

        assertThat(limpo).isEqualTo("[{\"id\": 1, \"nome\": \"Português\"}]");
    }

    @Test
    @DisplayName("Deve preservar JSON puro sem alterações indevidas")
    void devePreservarJsonPuro() {
        String jsonPuro = "{\"status\": \"OK\"}";
        String limpo = AiOrchestratorService.cleanJsonText(jsonPuro);
        assertThat(limpo).isEqualTo("{\"status\": \"OK\"}");
    }

    @Test
    @DisplayName("Deve auto-reparar JSON truncado com string e delimitadores não fechados")
    void deveRepararJsonTruncadoComStringAberta() {
        String truncado = "{\"nomeConcurso\": \"Concurso 2026\", \"cargos\": [\"Analista Judiciário";
        String reparado = AiOrchestratorService.cleanJsonText(truncado);

        assertThat(reparado).isEqualTo("{\"nomeConcurso\": \"Concurso 2026\", \"cargos\": [\"Analista Judiciário\"]}");
    }

    @Test
    @DisplayName("Deve auto-reparar JSON truncado com vírgula pendente")
    void deveRepararJsonTruncadoComVirgulaPendente() {
        String truncado = "{\"nomeConcurso\": \"Concurso 2026\", \"cargos\": [\"Analista\", ";
        String reparado = AiOrchestratorService.cleanJsonText(truncado);

        assertThat(reparado).isEqualTo("{\"nomeConcurso\": \"Concurso 2026\", \"cargos\": [\"Analista\"]}");
    }

    @Test
    @DisplayName("Deve auto-reparar bloco de código markdown não fechado")
    void deveRepararBlocoMarkdownTruncadoSemFechamento() {
        String truncado = "```json\n{\"nomeConcurso\": \"Concurso 2026\", \"cargos\": [\"Técnico";
        String reparado = AiOrchestratorService.cleanJsonText(truncado);

        assertThat(reparado).isEqualTo("{\"nomeConcurso\": \"Concurso 2026\", \"cargos\": [\"Técnico\"]}");
    }

    @Test
    @DisplayName("Deve acionar provedor secundário de fallback com sucesso quando provedor principal der 429")
    void deveChamarFallbackComSucessoQuandoProvedorPrincipalRetornar429() {
        PromptRequest request = new PromptRequest("Gerar tópicos de estudo");
        AiGenerationResponse respostaFallback = new AiGenerationResponse(
                "Resposta gerada pelo Groq (Llama 3)",
                "groq",
                "llama-3.3-70b-versatile",
                50L,
                100L,
                150L,
                java.time.Instant.now()
        );

        when(chatClientBuilder.build().prompt().user(request.prompt()).call())
                .thenThrow(new RuntimeException("429 RESOURCE_EXHAUSTED: Quota exceeded"));
        when(aiFallbackClientSupport.isFallbackDisponivel()).thenReturn(true);
        when(aiFallbackClientSupport.executeFallback(request)).thenReturn(respostaFallback);

        AiGenerationResponse resultado = orchestratorService.generate(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.content()).isEqualTo("Resposta gerada pelo Groq (Llama 3)");
        assertThat(resultado.provider()).isEqualTo("groq");
        verify(aiFallbackClientSupport).executeFallback(request);
    }

    @Test
    @DisplayName("Deve lançar AiQuotaExceededException quando erro de cota 429 persistir e fallback estiver indisponível")
    void deveLancarAiQuotaExceededExceptionQuandoErro429Persistir() {
        PromptRequest request = new PromptRequest("Gerar resumo");

        when(chatClientBuilder.build().prompt().user(request.prompt()).call())
                .thenThrow(new RuntimeException("429 RESOURCE_EXHAUSTED: Quota exceeded for quota metric"));
        when(aiFallbackClientSupport.isFallbackDisponivel()).thenReturn(false);

        assertThatThrownBy(() -> orchestratorService.generate(request))
                .isInstanceOf(com.kyofoundation.skillnapse.common.exception.AiQuotaExceededException.class)
                .hasMessageContaining("A cota diária de requisições de Inteligência Artificial foi atingida");
    }

    @Test
    @DisplayName("Deve acionar provedor secundário de fallback com sucesso quando provedor principal retornar erro 401 de credenciais")
    void deveChamarFallbackComSucessoQuandoProvedorPrincipalRetornar401CredenciaisInvalidas() {
        PromptRequest request = new PromptRequest("Gerar lote de questões");
        AiGenerationResponse respostaFallback = new AiGenerationResponse(
                "Resposta de questões via Groq",
                "groq",
                "llama-3.3-70b-versatile",
                30L,
                80L,
                110L,
                java.time.Instant.now()
        );

        when(chatClientBuilder.build().prompt().user(request.prompt()).call())
                .thenThrow(new RuntimeException("401 . Request had invalid authentication credentials. reason: ACCESS_TOKEN_TYPE_UNSUPPORTED"));
        when(aiFallbackClientSupport.isFallbackDisponivel()).thenReturn(true);
        when(aiFallbackClientSupport.executeFallback(request)).thenReturn(respostaFallback);

        AiGenerationResponse resultado = orchestratorService.generate(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.content()).isEqualTo("Resposta de questões via Groq");
        assertThat(resultado.provider()).isEqualTo("groq");
        verify(aiFallbackClientSupport).executeFallback(request);
    }
}
