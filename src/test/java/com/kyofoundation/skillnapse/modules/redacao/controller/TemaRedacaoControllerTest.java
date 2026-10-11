package com.kyofoundation.skillnapse.modules.redacao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kyofoundation.skillnapse.common.config.SecurityConfig;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.common.exception.GlobalExceptionHandler;
import com.kyofoundation.skillnapse.common.exception.ResourceNotFoundException;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.GerarTemaRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResumoResponse;
import com.kyofoundation.skillnapse.modules.redacao.service.TemaRedacaoService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemaRedacaoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TemaRedacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private TemaRedacaoService temaRedacaoService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve gerar tema de redação com status 201 Created")
    void deveGerarTemaComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        UUID temaId = UUID.randomUUID();

        GerarTemaRedacaoRequest request = new GerarTemaRedacaoRequest(
                planoId, null, null, "Cebraspe", "Dissertativo"
        );

        TemaRedacaoResponse response = new TemaRedacaoResponse(
                temaId,
                planoId,
                "Inteligência Artificial e Soberania Digital",
                "Contextualização da proposta de redação.",
                "Texto I: Infraestrutura crítica e dependência tecnológica.",
                "Aborde em até 30 linhas: 1) Riscos à segurança; 2) Regulação estatal.",
                true,
                Instant.now(),
                0L
        );

        when(temaRedacaoService.gerarTema(eq(userId), any(GerarTemaRedacaoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/redacoes/temas/gerar")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(temaId.toString()))
                .andExpect(jsonPath("$.titulo").value("Inteligência Artificial e Soberania Digital"))
                .andExpect(jsonPath("$.planoEstudoId").value(planoId.toString()))
                .andExpect(jsonPath("$.geradoPorIa").value(true));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando planoEstudoId for nulo")
    void deveRetornar400QuandoPlanoEstudoIdNulo() throws Exception {
        UUID userId = UUID.randomUUID();
        GerarTemaRedacaoRequest request = new GerarTemaRedacaoRequest(
                null, null, null, "Cebraspe", "Dissertativo"
        );

        mockMvc.perform(post("/api/v1/redacoes/temas/gerar")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized ao gerar sem JWT")
    void deveRetornar401SemJwt() throws Exception {
        GerarTemaRedacaoRequest request = new GerarTemaRedacaoRequest(
                UUID.randomUUID(), null, null, "Cebraspe", "Dissertativo"
        );

        mockMvc.perform(post("/api/v1/redacoes/temas/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário não for dono do plano")
    void deveRetornar403QuandoNaoForDonoDoPlano() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        GerarTemaRedacaoRequest request = new GerarTemaRedacaoRequest(
                planoId, null, null, "Cebraspe", "Dissertativo"
        );

        when(temaRedacaoService.gerarTema(eq(userId), any(GerarTemaRedacaoRequest.class)))
                .thenThrow(new ForbiddenException("Você não possui permissão para acessar este plano de estudo."));

        mockMvc.perform(post("/api/v1/redacoes/temas/gerar")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não possui permissão para acessar este plano de estudo."));
    }

    @Test
    @DisplayName("Deve listar temas de redação com status 200 OK")
    void deveListarTemasComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        UUID temaId = UUID.randomUUID();

        TemaRedacaoResumoResponse resumo = new TemaRedacaoResumoResponse(
                temaId,
                planoId,
                "Tema Resumido",
                true,
                Instant.now(),
                1L
        );

        when(temaRedacaoService.listarPorPlano(eq(userId), eq(planoId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resumo)));

        mockMvc.perform(get("/api/v1/redacoes/temas")
                        .param("planoEstudoId", planoId.toString())
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(temaId.toString()))
                .andExpect(jsonPath("$.content[0].titulo").value("Tema Resumido"))
                .andExpect(jsonPath("$.content[0].totalSubmissoes").value(1));
    }

    @Test
    @DisplayName("Deve obter tema por ID com status 200 OK")
    void deveObterTemaPorIdComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID temaId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();

        TemaRedacaoResponse response = new TemaRedacaoResponse(
                temaId,
                planoId,
                "Tema Detalhado",
                "Contextualização detalhada do tema.",
                "Texto motivador I e II",
                "Critérios de pontuação",
                true,
                Instant.now(),
                3L
        );

        when(temaRedacaoService.obterPorId(eq(userId), eq(temaId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/redacoes/temas/{id}", temaId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(temaId.toString()))
                .andExpect(jsonPath("$.titulo").value("Tema Detalhado"))
                .andExpect(jsonPath("$.totalSubmissoes").value(3));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando tema não for encontrado")
    void deveRetornar404QuandoTemaNaoEncontrado() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID temaId = UUID.randomUUID();

        when(temaRedacaoService.obterPorId(eq(userId), eq(temaId)))
                .thenThrow(new ResourceNotFoundException("Tema de redação não encontrado com o id: " + temaId));

        mockMvc.perform(get("/api/v1/redacoes/temas/{id}", temaId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tema de redação não encontrado com o id: " + temaId));
    }
}
