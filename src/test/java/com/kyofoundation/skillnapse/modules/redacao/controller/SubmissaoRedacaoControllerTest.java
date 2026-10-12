package com.kyofoundation.skillnapse.modules.redacao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kyofoundation.skillnapse.common.config.SecurityConfig;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.common.exception.GlobalExceptionHandler;
import com.kyofoundation.skillnapse.common.exception.ResourceNotFoundException;
import com.kyofoundation.skillnapse.modules.redacao.dto.payload.FeedbackCorrecaoIaPayload;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.SubmeterRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.SubmissaoRedacaoResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.SubmissaoRedacaoResumoResponse;
import com.kyofoundation.skillnapse.modules.redacao.service.SubmissaoRedacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissaoRedacaoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SubmissaoRedacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmissaoRedacaoService submissaoRedacaoService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve submeter redação com status 201 Created")
    void deveSubmeterRedacaoComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID submissaoId = UUID.randomUUID();
        UUID temaId = UUID.randomUUID();

        String texto = "A modernização tecnológica dos serviços públicos é fundamental para assegurar a cidadania...".repeat(4);
        SubmeterRedacaoRequest request = new SubmeterRedacaoRequest(temaId, texto);

        FeedbackCorrecaoIaPayload feedback = new FeedbackCorrecaoIaPayload(
                8.80, List.of(), "Muito bom", List.of()
        );

        SubmissaoRedacaoResponse response = new SubmissaoRedacaoResponse(
                submissaoId,
                temaId,
                "Inteligência Artificial e Cidadania",
                texto,
                new BigDecimal("8.80"),
                feedback,
                Instant.now(),
                Instant.now()
        );

        when(submissaoRedacaoService.submeterRedacao(eq(userId), any(SubmeterRedacaoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/redacoes/submissoes")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(submissaoId.toString()))
                .andExpect(jsonPath("$.temaId").value(temaId.toString()))
                .andExpect(jsonPath("$.notaGeral").value(8.80));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando texto for menor que 300 caracteres")
    void deveRetornar400QuandoTextoMuitoCurto() throws Exception {
        UUID userId = UUID.randomUUID();
        SubmeterRedacaoRequest request = new SubmeterRedacaoRequest(UUID.randomUUID(), "Texto muito curto.");

        mockMvc.perform(post("/api/v1/redacoes/submissoes")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized ao submeter sem autenticação")
    void deveRetornar401SemJwt() throws Exception {
        SubmeterRedacaoRequest request = new SubmeterRedacaoRequest(UUID.randomUUID(), "Texto de teste...".repeat(30));

        mockMvc.perform(post("/api/v1/redacoes/submissoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário não tiver acesso ao tema")
    void deveRetornar403QuandoNaoTiverAcessoAoTema() throws Exception {
        UUID userId = UUID.randomUUID();
        SubmeterRedacaoRequest request = new SubmeterRedacaoRequest(UUID.randomUUID(), "Texto de teste...".repeat(30));

        when(submissaoRedacaoService.submeterRedacao(eq(userId), any(SubmeterRedacaoRequest.class)))
                .thenThrow(new ForbiddenException("Você não possui permissão para submeter redação para este tema."));

        mockMvc.perform(post("/api/v1/redacoes/submissoes")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não possui permissão para submeter redação para este tema."));
    }

    @Test
    @DisplayName("Deve listar submissões com status 200 OK")
    void deveListarSubmissoesComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID submissaoId = UUID.randomUUID();
        UUID temaId = UUID.randomUUID();

        SubmissaoRedacaoResumoResponse resumo = new SubmissaoRedacaoResumoResponse(
                submissaoId,
                temaId,
                "Tema Teste",
                new BigDecimal("9.00"),
                Instant.now(),
                Instant.now()
        );

        when(submissaoRedacaoService.listar(eq(userId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resumo)));

        mockMvc.perform(get("/api/v1/redacoes/submissoes")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(submissaoId.toString()))
                .andExpect(jsonPath("$.content[0].notaGeral").value(9.00));
    }

    @Test
    @DisplayName("Deve obter submissão por ID com status 200 OK")
    void deveObterSubmissaoPorIdComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID submissaoId = UUID.randomUUID();
        UUID temaId = UUID.randomUUID();

        SubmissaoRedacaoResponse response = new SubmissaoRedacaoResponse(
                submissaoId,
                temaId,
                "Tema Detalhado",
                "Texto completo da redação",
                new BigDecimal("9.50"),
                null,
                Instant.now(),
                Instant.now()
        );

        when(submissaoRedacaoService.obterPorId(eq(userId), eq(submissaoId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/redacoes/submissoes/{id}", submissaoId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissaoId.toString()))
                .andExpect(jsonPath("$.notaGeral").value(9.50));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando submissão não for encontrada")
    void deveRetornar404QuandoSubmissaoNaoEncontrada() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID submissaoId = UUID.randomUUID();

        when(submissaoRedacaoService.obterPorId(eq(userId), eq(submissaoId)))
                .thenThrow(new ResourceNotFoundException("Submissão de redação não encontrada com o id: " + submissaoId));

        mockMvc.perform(get("/api/v1/redacoes/submissoes/{id}", submissaoId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Submissão de redação não encontrada com o id: " + submissaoId));
    }

    @Test
    @DisplayName("Deve submeter imagem de redação manuscrita com status 200 OK")
    void deveSubmeterImagemComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID temaId = UUID.randomUUID();
        MockMultipartFile imagem = new MockMultipartFile(
                "imagem", "redacao.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        com.kyofoundation.skillnapse.modules.redacao.dto.response.ResultadoSubmissaoImagemResponse response =
                new com.kyofoundation.skillnapse.modules.redacao.dto.response.ResultadoSubmissaoImagemResponse(
                        true,
                        91.0,
                        "Redação manuscrita transcrita e corrigida com sucesso!",
                        "Texto transcrito da folha...",
                        null
                );

        when(submissaoRedacaoService.submeterRedacaoImagem(eq(userId), eq(temaId), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/redacoes/submissoes/imagem")
                        .file(imagem)
                        .param("temaRedacaoId", temaId.toString())
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legivel").value(true))
                .andExpect(jsonPath("$.percentualLegibilidade").value(91.0))
                .andExpect(jsonPath("$.textoTranscrito").value("Texto transcrito da folha..."));
    }

    @Test
    @DisplayName("Deve excluir submissão de redação com status 204 No Content")
    void deveExcluirSubmissaoComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID submissaoId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/redacoes/submissoes/{id}", submissaoId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }
}
