package com.kyofoundation.skillnapse.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyofoundation.skillnapse.common.config.SecurityConfig;
import com.kyofoundation.skillnapse.common.exception.GlobalExceptionHandler;
import com.kyofoundation.skillnapse.modules.auth.dto.request.LoginRequest;
import com.kyofoundation.skillnapse.modules.auth.dto.request.RefreshTokenRequest;
import com.kyofoundation.skillnapse.modules.auth.dto.request.RegistroRequest;
import com.kyofoundation.skillnapse.modules.auth.dto.response.LoginResponse;
import com.kyofoundation.skillnapse.modules.auth.dto.response.RegistroResponse;
import com.kyofoundation.skillnapse.modules.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Deve registrar usuario e retornar 200 OK")
    void deveRegistrarUsuarioComSucesso() throws Exception {
        RegistroRequest request = new RegistroRequest("Aluno Kyo", "aluno@skillnapse.com", "senhaSegura123");
        RegistroResponse response = new RegistroResponse(UUID.randomUUID(), "Aluno Kyo", "aluno@skillnapse.com");

        when(authService.register(any(RegistroRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Aluno Kyo"))
                .andExpect(jsonPath("$.email").value("aluno@skillnapse.com"));
    }

    @Test
    @DisplayName("Deve logar usuario e retornar 200 OK com tokens")
    void deveLogarUsuarioComSucesso() throws Exception {
        LoginRequest request = new LoginRequest("aluno@skillnapse.com", "senhaSegura123");
        LoginResponse response = new LoginResponse(
                UUID.randomUUID(),
                "Aluno Kyo",
                "aluno@skillnapse.com",
                "mock-access-token",
                "mock-refresh-token",
                "Bearer",
                3600L
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Deve renovar token via POST /auth/refresh e retornar 200 OK")
    void deveRenovarTokenComSucesso() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("mock-refresh-token");
        LoginResponse response = new LoginResponse(
                UUID.randomUUID(),
                "Aluno Kyo",
                "aluno@skillnapse.com",
                "novo-access-token",
                "novo-refresh-token",
                "Bearer",
                3600L
        );

        when(authService.renovarToken("mock-refresh-token")).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("novo-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("novo-refresh-token"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request ao tentar renovar com refresh token vazio")
    void deveRetornarBadRequestQuandoRefreshTokenVazio() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request ao registrar com nome excedendo 150 caracteres")
    void deveRetornarBadRequestQuandoNomeExceder150Caracteres() throws Exception {
        String nomeLongo = "A".repeat(151);
        RegistroRequest request = new RegistroRequest(nomeLongo, "aluno@skillnapse.com", "senhaSegura123");

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    @DisplayName("Deve retornar dados do usuario autenticado via GET /auth/me com 200 OK")
    void deveRetornarUsuarioAutenticadoComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        RegistroResponse response = new RegistroResponse(userId, "Aluno Kyo", "aluno@skillnapse.com");

        when(authService.obterUsuarioAutenticado(userId)).thenReturn(response);

        mockMvc.perform(get("/auth/me")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.nome").value("Aluno Kyo"))
                .andExpect(jsonPath("$.email").value("aluno@skillnapse.com"));
    }
}
