package com.kyofoundation.skillnapse.modules.planoestudo.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.AtualizarProgressoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EditarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.ReordenarItensRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.TopicoResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.service.TopicoService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/topicos")
@RequiredArgsConstructor
@Tag(name = "Tópicos e Árvore de Conhecimento", description = "Endpoints para gerenciamento hierárquico de tópicos, subtópicos e proficiência")
@SecurityRequirement(name = "bearerAuth")
public class TopicoController {

    private final TopicoService topicoService;

    @PostMapping("/materia/{materiaId}")
    @Operation(summary = "Criar tópico ou subtópico", description = "Cadastra um tópico raiz ou aninhado (subtópico via topicoPaiId) na matéria informada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tópico cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = TopicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados do tópico inválidos (ex: peso fora de 1 a 5 ou tópico pai de outra matéria)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: matéria não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Matéria ou tópico pai não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TopicoResponse> criarTopico(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materiaId,
            @Valid @RequestBody CriarTopicoRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(topicoService.criarTopico(userId, materiaId, request));
    }

    @GetMapping("/{topicoId}")
    @Operation(summary = "Visualizar tópico", description = "Recupera os detalhes de um tópico ou subtópico individual por ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tópico recuperado com sucesso",
                    content = @Content(schema = @Schema(implementation = TopicoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: tópico não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TopicoResponse> visualizarTopico(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID topicoId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(topicoService.visualizarTopico(userId, topicoId));
    }

    @PatchMapping("/{topicoId}")
    @Operation(summary = "Editar tópico", description = "Atualiza título, peso relativo no edital (1 a 5), status ou ordenação do tópico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tópico atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = TopicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de edição inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: tópico não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TopicoResponse> editarTopico(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID topicoId,
            @Valid @RequestBody EditarTopicoRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(topicoService.editarTopico(userId, topicoId, request));
    }

    @PatchMapping("/{topicoId}/progresso")
    @Operation(summary = "Atualizar progresso e proficiência", description = "Marca o tópico como concluído e/ou atualiza o nível de proficiência (INICIANTE, INTERMEDIARIO, AVANCADO).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progresso do tópico atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = TopicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (nenhum dado de progresso informado)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: tópico não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TopicoResponse> atualizarStatusEProficiencia(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID topicoId,
            @Valid @RequestBody AtualizarProgressoRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(topicoService.atualizarStatusEProficiencia(userId, topicoId, request));
    }

    @DeleteMapping("/{topicoId}")
    @Operation(summary = "Deletar tópico", description = "Remove o tópico e todos os subtópicos aninhados em cascata.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tópico removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: tópico não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deletarTopico(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID topicoId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        topicoService.deletarTopico(userId, topicoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/materia/{materiaId}")
    @Operation(summary = "Listar árvore hierárquica de tópicos", description = "Retorna os tópicos raiz da matéria, com todos os seus subtópicos aninhados recursivamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Árvore de tópicos recuperada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TopicoResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: matéria não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Matéria não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<TopicoResponse>> listarTopicosPorMateria(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materiaId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(topicoService.listarArvoreTopicosPorMateria(userId, materiaId));
    }

    @PatchMapping("/materia/{materiaId}/reordenar")
    @Operation(summary = "Reordenar tópicos da matéria", description = "Atualiza a ordenação sequencial de tópicos ou subtópicos de uma disciplina.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem dos tópicos atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Lista de identificadores vazia ou inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido: matéria ou tópico não pertence ao usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Matéria ou tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> reordenarTopicos(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materiaId,
            @Valid @RequestBody ReordenarItensRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        topicoService.reordenarTopicos(userId, materiaId, request);
        return ResponseEntity.noContent().build();
    }
}
