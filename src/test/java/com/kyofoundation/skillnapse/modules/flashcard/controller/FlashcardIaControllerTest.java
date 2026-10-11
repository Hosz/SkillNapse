package com.kyofoundation.skillnapse.modules.flashcard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kyofoundation.skillnapse.common.config.SecurityConfig;
import com.kyofoundation.skillnapse.common.exception.GlobalExceptionHandler;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.GerarFlashcardsIaRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.FlashcardResponse;
import com.kyofoundation.skillnapse.modules.flashcard.service.FlashcardIaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlashcardIaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FlashcardIaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private FlashcardIaService flashcardIaService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve gerar flashcards por IA com status 201 Created")
    void deveGerarFlashcardsPorIaComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID planoId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID baralhoId = UUID.randomUUID();

        GerarFlashcardsIaRequest request = new GerarFlashcardsIaRequest(
                planoId, null, null, null, null, 5, baralhoId
        );

        FlashcardResponse cardResponse = new FlashcardResponse(
                cardId,
                baralhoId,
                "Baralho Direito Constitucional",
                null,
                null,
                "O que é Habeas Corpus?",
                "Remédio constitucional para proteger a liberdade de locomoção.",
                new BigDecimal("2.50"),
                0,
                0,
                LocalDate.now(),
                null
        );

        when(flashcardIaService.gerarFlashcardsIa(eq(userId), any(GerarFlashcardsIaRequest.class)))
                .thenReturn(List.of(cardResponse));

        mockMvc.perform(post("/api/v1/flashcards/ia/gerar")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(cardId.toString()))
                .andExpect(jsonPath("$[0].frente").value("O que é Habeas Corpus?"))
                .andExpect(jsonPath("$[0].verso").value("Remédio constitucional para proteger a liberdade de locomoção."));
    }

    @Test
    @DisplayName("Deve retornar status 400 Bad Request ao omitir planoId obrigatório")
    void deveRetornarBadRequestQuandoPlanoIdEstiverAusente() throws Exception {
        UUID userId = UUID.randomUUID();
        GerarFlashcardsIaRequest requestInvalido = new GerarFlashcardsIaRequest(
                null, null, null, null, null, 5, null
        );

        mockMvc.perform(post("/api/v1/flashcards/ia/gerar")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar status 401 Unauthorized se requisição não contiver JWT")
    void deveRetornarUnauthorizedSemJwt() throws Exception {
        GerarFlashcardsIaRequest request = new GerarFlashcardsIaRequest(
                UUID.randomUUID(), null, null, null, null, 5, null
        );

        mockMvc.perform(post("/api/v1/flashcards/ia/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
