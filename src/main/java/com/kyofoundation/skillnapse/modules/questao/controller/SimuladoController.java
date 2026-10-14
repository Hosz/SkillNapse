package com.kyofoundation.skillnapse.modules.questao.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarSimuladoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.SimuladoResponse;
import com.kyofoundation.skillnapse.modules.questao.service.SimuladoService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulados")
@RequiredArgsConstructor
@Tag(name = "Simulados", description = "Endpoints para gerenciamento de simulados manuais e adaptativos")
@SecurityRequirement(name = "bearerAuth")
public class SimuladoController {

    private final SimuladoService simuladoService;

    @PostMapping
    @Operation(summary = "Criar simulado", description = "Cadastra um novo simulado vinculado ao usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Simulado criado com sucesso",
                    content = @Content(schema = @Schema(implementation = SimuladoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados do simulado inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SimuladoResponse> criar(
            @Valid @RequestBody CriarSimuladoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(simuladoService.criar(request, userId));
    }

    @GetMapping
    @Operation(summary = "Listar simulados", description = "Lista os simulados do usuário autenticado de forma paginada com estatísticas de desempenho.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulados retornados com sucesso")
    })
    public ResponseEntity<Page<SimuladoResponse>> listar(
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(simuladoService.listarPorUsuario(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter simulado por ID", description = "Retorna detalhes e métricas consolidadas de um simulado específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulado retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = SimuladoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Simulado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Simulado não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SimuladoResponse> buscarPorId(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(simuladoService.buscarPorId(id, userId));
    }

    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Concluir simulado", description = "Finaliza o simulado, congelando suas respostas e calculando o aproveitamento final.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulado concluído com sucesso",
                    content = @Content(schema = @Schema(implementation = SimuladoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Simulado já estava concluído",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Simulado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Simulado não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SimuladoResponse> concluir(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(simuladoService.concluir(id, userId));
    }

    @PatchMapping("/{id}/reiniciar")
    @Operation(summary = "Reiniciar simulado", description = "Reabre um simulado para que o usuário possa refazê-lo do início.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulado reiniciado com sucesso",
                    content = @Content(schema = @Schema(implementation = SimuladoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Simulado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Simulado não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SimuladoResponse> reiniciar(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(simuladoService.reiniciar(id, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir simulado", description = "Remove um simulado do histórico do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Simulado excluído com sucesso"),
            @ApiResponse(responseCode = "403", description = "Simulado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Simulado não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> excluir(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        simuladoService.excluir(id, userId);
        return ResponseEntity.noContent().build();
    }
}
