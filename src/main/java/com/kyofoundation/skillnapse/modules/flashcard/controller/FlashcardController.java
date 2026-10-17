package com.kyofoundation.skillnapse.modules.flashcard.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.AtualizarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.CriarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.RevisarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.FlashcardResponse;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.HistoricoRevisaoResponse;
import com.kyofoundation.skillnapse.modules.flashcard.service.FlashcardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flashcards")
@RequiredArgsConstructor
@Tag(name = "Flashcards SRS (Repetição Espaçada)", description = "Endpoints para gerenciamento de cartões de estudo e execução do algoritmo SM-2")
@SecurityRequirement(name = "bearerAuth")
public class FlashcardController {

    private final FlashcardService flashcardService;

    @PostMapping
    @Operation(summary = "Criar flashcard", description = "Cadastra um novo flashcard associado a um baralho e opcionalmente a um tópico.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flashcard criado com sucesso",
                    content = @Content(schema = @Schema(implementation = FlashcardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados do cartão inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Baralho ou tópico pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Baralho ou tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FlashcardResponse> criarFlashcard(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CriarFlashcardRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(flashcardService.criarFlashcard(userId, request));
    }

    @GetMapping
    @Operation(summary = "Listar flashcards do usuário", description = "Lista os cartões do usuário autenticado com filtros opcionais por baralho ou tópico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de cartões recuperada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Baralho ou tópico informado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Baralho ou tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<FlashcardResponse>> listarFlashcards(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do baralho (opcional)") @RequestParam(required = false) UUID baralhoId,
            @Parameter(description = "ID do tópico (opcional)") @RequestParam(required = false) UUID topicoId,
            @ParameterObject Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(flashcardService.listarFlashcards(userId, baralhoId, topicoId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter flashcard por ID", description = "Recupera as informações detalhadas e estado do algoritmo SM-2 de um flashcard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flashcard recuperado com sucesso",
                    content = @Content(schema = @Schema(implementation = FlashcardResponse.class))),
            @ApiResponse(responseCode = "403", description = "Flashcard pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Flashcard não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FlashcardResponse> obterFlashcard(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do flashcard") @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(flashcardService.obterFlashcard(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar flashcard", description = "Atualiza a frente, verso ou tópico acoplado de um flashcard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flashcard atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = FlashcardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Flashcard ou tópico pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Flashcard ou tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FlashcardResponse> atualizarFlashcard(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do flashcard") @PathVariable UUID id,
            @Valid @RequestBody AtualizarFlashcardRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(flashcardService.atualizarFlashcard(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Apagar flashcard", description = "Remove um flashcard e seus registros históricos de revisão.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Flashcard removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Flashcard pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Flashcard não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> apagarFlashcard(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do flashcard") @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        flashcardService.apagarFlashcard(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/revisar")
    @Operation(summary = "Revisar flashcard (SM-2)", description = "Processa a resposta do usuário, calcula novo fator de facilidade, intervalo de dias e agenda a próxima revisão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revisão processada e cartão atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = FlashcardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Classificação de resposta inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Flashcard pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Flashcard não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FlashcardResponse> revisarFlashcard(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do flashcard") @PathVariable UUID id,
            @Valid @RequestBody RevisarFlashcardRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(flashcardService.revisarFlashcard(userId, id, request));
    }

    @GetMapping("/due")
    @Operation(summary = "Listar cards vencidos para revisão", description = "Lista os cartões que vencem até a data atual (proximaRevisao <= hoje), com filtros opcionais por baralho e tópico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de cartões a revisar recuperada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Baralho ou tópico informado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Baralho ou tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<FlashcardResponse>> listarCardsVencidos(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do baralho (opcional)") @RequestParam(required = false) UUID baralhoId,
            @Parameter(description = "ID do tópico (opcional)") @RequestParam(required = false) UUID topicoId,
            @ParameterObject Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(flashcardService.listarCardsVencidos(userId, baralhoId, topicoId, pageable));
    }

    @GetMapping("/baralho/{baralhoId}")
    @Operation(summary = "Listar flashcards de um baralho", description = "Lista todos os flashcards pertencentes a um baralho específico com paginação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de flashcards recuperada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Baralho pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Baralho não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<FlashcardResponse>> listarFlashcardsPorBaralho(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do baralho") @PathVariable UUID baralhoId,
            @ParameterObject Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(flashcardService.listarFlashcardsPorBaralho(userId, baralhoId, pageable));
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Listar histórico de revisões de um flashcard", description = "Lista as tentativas e revisões anteriores realizadas para o cartão especificado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico recuperado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Flashcard pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Flashcard não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<HistoricoRevisaoResponse>> listarHistoricoRevisoes(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do flashcard") @PathVariable UUID id,
            @ParameterObject Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(flashcardService.listarHistoricoRevisoes(userId, id, pageable));
    }
}
