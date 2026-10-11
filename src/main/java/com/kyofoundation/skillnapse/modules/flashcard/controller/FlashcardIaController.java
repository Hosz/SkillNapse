package com.kyofoundation.skillnapse.modules.flashcard.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.GerarFlashcardsIaRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.FlashcardResponse;
import com.kyofoundation.skillnapse.modules.flashcard.service.FlashcardIaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flashcards/ia")
@RequiredArgsConstructor
@Tag(name = "Flashcards por IA", description = "Endpoints para síntese e geração pedagógica de cartões de estudo via Inteligência Artificial")
@SecurityRequirement(name = "bearerAuth")
public class FlashcardIaController {

    private final FlashcardIaService flashcardIaService;

    @PostMapping("/gerar")
    @Operation(summary = "Gerar flashcards por IA", description = "Sintetiza cartões conceituais atômicos baseados no plano de estudos, matéria, tópico ou subtópico informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flashcards gerados e persistidos com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FlashcardResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao plano ou matéria",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plano, tópico ou baralho não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<FlashcardResponse>> gerar(
            @Valid @RequestBody GerarFlashcardsIaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID usuarioId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flashcardIaService.gerarFlashcardsIa(usuarioId, request));
    }
}
