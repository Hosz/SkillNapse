package com.kyofoundation.skillnapse.modules.planoestudo.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarMateriaRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EditarMateriaRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.ReordenarItensRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.MateriaResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.service.MateriaService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/materias")
@RequiredArgsConstructor
@Tag(name = "Matérias", description = "Endpoints para gerenciamento de matérias e disciplinas vinculadas aos planos de estudo")
@SecurityRequirement(name = "bearerAuth")
public class MateriaController {

    private final MateriaService materiaService;

    @PostMapping("/plano/{planoId}")
    @Operation(summary = "Criar matéria", description = "Cadastra uma nova disciplina vinculada a um plano de estudos específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Matéria cadastrada com sucesso",
                    content = @Content(schema = @Schema(implementation = MateriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da matéria inválidos (ex: cor hex fora do padrão)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: plano não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plano de estudo não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MateriaResponse> criarMateria(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID planoId,
            @Valid @RequestBody CriarMateriaRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(materiaService.criarMateria(userId, planoId, request));
    }

    @GetMapping("/{materiaId}")
    @Operation(summary = "Visualizar matéria", description = "Busca detalhes de uma matéria através do seu ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matéria recuperada com sucesso",
                    content = @Content(schema = @Schema(implementation = MateriaResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: matéria não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Matéria não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MateriaResponse> verMateria(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materiaId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(materiaService.verMateria(userId, materiaId));
    }

    @PatchMapping("/{materiaId}")
    @Operation(summary = "Editar matéria", description = "Atualiza parcialmente o nome, cor hexadecimal (#RRGGBB) ou ordenação da matéria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matéria atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = MateriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de edição inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: matéria não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Matéria não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MateriaResponse> editarMateria(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materiaId,
            @Valid @RequestBody EditarMateriaRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(materiaService.editarMateria(userId, materiaId, request));
    }

    @DeleteMapping("/{materiaId}")
    @Operation(summary = "Apagar matéria", description = "Remove a matéria e todos os tópicos e subtópicos vinculados em cascata.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Matéria removida com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: matéria não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Matéria não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> apagarMateria(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materiaId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        materiaService.apagarMateria(userId, materiaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/plano/{planoId}")
    @Operation(summary = "Listar matérias do plano", description = "Lista paginada de todas as disciplinas vinculadas a um determinado plano de estudo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de matérias recuperada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: plano não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plano de estudo não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<MateriaResponse>> listarMaterias(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID planoId,
            Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(materiaService.listarMaterias(userId, planoId, pageable));
    }

    @PatchMapping("/plano/{planoId}/reordenar")
    @Operation(summary = "Reordenar matérias", description = "Atualiza a ordenação sequencial das matérias vinculadas ao plano de estudos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem das matérias atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Lista de identificadores vazia ou inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: plano ou matéria não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plano de estudo ou matéria não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> reordenarMaterias(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID planoId,
            @Valid @RequestBody ReordenarItensRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        materiaService.reordenarMaterias(userId, planoId, request);
        return ResponseEntity.noContent().build();
    }
}
