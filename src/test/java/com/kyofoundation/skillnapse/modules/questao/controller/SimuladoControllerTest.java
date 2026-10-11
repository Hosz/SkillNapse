package com.kyofoundation.skillnapse.modules.questao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kyofoundation.skillnapse.common.config.SecurityConfig;
import com.kyofoundation.skillnapse.common.exception.GlobalExceptionHandler;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarSimuladoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.SimuladoResponse;
import com.kyofoundation.skillnapse.modules.questao.enums.TipoSimulado;
import com.kyofoundation.skillnapse.modules.questao.service.SimuladoService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimuladoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SimuladoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private SimuladoService simuladoService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve criar simulado com status 201 Created")
    void deveCriarSimuladoComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID simId = UUID.randomUUID();
        CriarSimuladoRequest request = new CriarSimuladoRequest("Simulado TCU Auditor", TipoSimulado.MANUAL);
        SimuladoResponse response = new SimuladoResponse(
                simId,
                "Simulado TCU Auditor",
                TipoSimulado.MANUAL,
                false,
                Instant.now(),
                0L,
                0L,
                0.0
        );

        when(simuladoService.criar(any(CriarSimuladoRequest.class), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/simulados")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(simId.toString()))
                .andExpect(jsonPath("$.titulo").value("Simulado TCU Auditor"))
                .andExpect(jsonPath("$.concluido").value(false));
    }

    @Test
    @DisplayName("Deve listar simulados com status 200 OK")
    void deveListarSimuladosComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        SimuladoResponse response = new SimuladoResponse(
                UUID.randomUUID(),
                "Simulado PF",
                TipoSimulado.MANUAL,
                true,
                Instant.now(),
                50L,
                42L,
                84.0
        );

        when(simuladoService.listarPorUsuario(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/simulados")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Simulado PF"))
                .andExpect(jsonPath("$.content[0].percentualAcerto").value(84.0));
    }

    @Test
    @DisplayName("Deve buscar simulado por ID com status 200 OK")
    void deveBuscarSimuladoPorId() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID simId = UUID.randomUUID();
        SimuladoResponse response = new SimuladoResponse(
                simId,
                "Simulado Detalhado",
                TipoSimulado.MANUAL,
                false,
                Instant.now(),
                10L,
                8L,
                80.0
        );

        when(simuladoService.buscarPorId(simId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/simulados/{id}", simId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(simId.toString()))
                .andExpect(jsonPath("$.totalAcertos").value(8));
    }

    @Test
    @DisplayName("Deve concluir simulado com status 200 OK")
    void deveConcluirSimulado() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID simId = UUID.randomUUID();
        SimuladoResponse response = new SimuladoResponse(
                simId,
                "Simulado Concluído",
                TipoSimulado.MANUAL,
                true,
                Instant.now(),
                20L,
                18L,
                90.0
        );

        when(simuladoService.concluir(simId, userId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/simulados/{id}/concluir", simId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(simId.toString()))
                .andExpect(jsonPath("$.concluido").value(true))
                .andExpect(jsonPath("$.percentualAcerto").value(90.0));
    }

    @Test
    @DisplayName("Deve excluir simulado com status 204 No Content")
    void deveExcluirSimuladoComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID simId = UUID.randomUUID();

        doNothing().when(simuladoService).excluir(simId, userId);

        mockMvc.perform(delete("/api/v1/simulados/{id}", simId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }
}
