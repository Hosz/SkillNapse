package com.kyofoundation.skillnapse.modules.sessao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kyofoundation.skillnapse.common.config.SecurityConfig;
import com.kyofoundation.skillnapse.common.exception.GlobalExceptionHandler;
import com.kyofoundation.skillnapse.modules.sessao.dto.request.RegistrarSessaoEstudoRequest;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.ResumoHorasLiquidasResponse;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.SessaoEstudoResponse;
import com.kyofoundation.skillnapse.modules.sessao.enums.StatusSessaoEstudo;
import com.kyofoundation.skillnapse.modules.sessao.service.SessaoEstudoService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessaoEstudoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SessaoEstudoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private SessaoEstudoService sessaoEstudoService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve registrar sessao de estudo com status 201 Created")
    void deveRegistrarSessaoEstudoComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID topicoId = UUID.randomUUID();
        Instant agora = Instant.now();

        RegistrarSessaoEstudoRequest request = new RegistrarSessaoEstudoRequest(
                topicoId,
                agora.minus(30, ChronoUnit.MINUTES),
                agora,
                1500,
                StatusSessaoEstudo.CONCLUIDA,
                "Foco total"
        );

        SessaoEstudoResponse response = new SessaoEstudoResponse(
                UUID.randomUUID(),
                topicoId,
                "Cinemática",
                UUID.randomUUID(),
                "Física",
                request.iniciadoEm(),
                request.finalizadoEm(),
                1500,
                StatusSessaoEstudo.CONCLUIDA,
                "Foco total"
        );

        when(sessaoEstudoService.registrarSessaoEstudo(eq(userId), any(RegistrarSessaoEstudoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/sessoes-estudo")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.topicoTitulo").value("Cinemática"))
                .andExpect(jsonPath("$.duracaoLiquidaSegundos").value(1500));
    }

    @Test
    @DisplayName("Deve listar historico de sessoes com status 200 OK")
    void deveListarHistoricoSessoesComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        SessaoEstudoResponse response = new SessaoEstudoResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cinemática",
                UUID.randomUUID(),
                "Física",
                Instant.now().minusSeconds(1800),
                Instant.now(),
                1500,
                StatusSessaoEstudo.CONCLUIDA,
                null
        );

        when(sessaoEstudoService.listarHistoricoSessoesEstudo(eq(userId), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/sessoes-estudo")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].topicoTitulo").value("Cinemática"));
    }

    @Test
    @DisplayName("Deve obter resumo de horas liquidas com status 200 OK")
    void deveObterResumoHorasComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        ResumoHorasLiquidasResponse response = new ResumoHorasLiquidasResponse(
                7200L,
                2.0,
                2L,
                0L,
                2L
        );

        when(sessaoEstudoService.obterResumoHoras(eq(userId), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/sessoes-estudo/resumo")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSegundosLiquidos").value(7200))
                .andExpect(jsonPath("$.totalHorasLiquidas").value(2.0))
                .andExpect(jsonPath("$.totalSessoes").value(2));
    }

    @Test
    @DisplayName("Deve obter resumo de horas de hoje com status 200 OK")
    void deveObterResumoHojeComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        ResumoHorasLiquidasResponse response = new ResumoHorasLiquidasResponse(
                3600L,
                1.0,
                1L,
                0L,
                1L
        );

        when(sessaoEstudoService.obterResumoHoje(eq(userId), eq("America/Sao_Paulo")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/sessoes-estudo/resumo/hoje")
                        .param("timezone", "America/Sao_Paulo")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSegundosLiquidos").value(3600))
                .andExpect(jsonPath("$.totalHorasLiquidas").value(1.0))
                .andExpect(jsonPath("$.totalSessoes").value(1));
    }

    @Test
    @DisplayName("Deve apagar sessao de estudo com status 204 No Content")
    void deveApagarSessaoComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessaoId = UUID.randomUUID();

        doNothing().when(sessaoEstudoService).apagarSessaoEstudo(userId, sessaoId);

        mockMvc.perform(delete("/api/v1/sessoes-estudo/{sessaoId}", sessaoId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }
}
