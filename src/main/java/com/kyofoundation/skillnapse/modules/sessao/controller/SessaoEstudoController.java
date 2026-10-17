package com.kyofoundation.skillnapse.modules.sessao.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.sessao.dto.request.RegistrarSessaoEstudoRequest;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.ResumoHorasLiquidasResponse;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.SessaoEstudoResponse;
import com.kyofoundation.skillnapse.modules.sessao.service.SessaoEstudoService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessoes-estudo")
@RequiredArgsConstructor
@Tag(name = "Sessões de Estudo (Pomodoro)", description = "Endpoints para registro consolidado de horas líquidas de foco e pomodoro")
@SecurityRequirement(name = "bearerAuth")
public class SessaoEstudoController {

    private final SessaoEstudoService sessaoEstudoService;

    @PostMapping
    @Operation(summary = "Registrar sessão de estudo", description = "Grava uma sessão consolidada de estudo (Pomodoro finalizado ou interrompido).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sessão de estudo registrada com sucesso",
                    content = @Content(schema = @Schema(implementation = SessaoEstudoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou inconsistência de horários/duração",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Tópico indicado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SessaoEstudoResponse> registrarSessaoEstudo(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegistrarSessaoEstudoRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(sessaoEstudoService.registrarSessaoEstudo(userId, request));
    }

    @GetMapping
    @Operation(summary = "Listar histórico de sessões", description = "Lista as sessões de estudo do usuário com paginação e filtros opcionais por tópico e período.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico de sessões recuperado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Período temporal com data inicial posterior à final",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Tópico informado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<SessaoEstudoResponse>> listarHistoricoSessoesEstudo(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do tópico para filtrar sessões específicas")
            @RequestParam(required = false) UUID topicoId,
            @Parameter(description = "Data/hora inicial em formato ISO-8601 UTC (ex: 2026-10-01T00:00:00Z)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @Parameter(description = "Data/hora final em formato ISO-8601 UTC (ex: 2026-10-07T23:59:59Z)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @ParameterObject Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(sessaoEstudoService.listarHistoricoSessoesEstudo(userId, topicoId, de, ate, pageable));
    }

    @GetMapping("/{sessaoId}")
    @Operation(summary = "Visualizar sessão de estudo", description = "Retorna os detalhes de uma sessão de estudo específica pelo seu identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão de estudo encontrada com sucesso",
                    content = @Content(schema = @Schema(implementation = SessaoEstudoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido a sessão de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sessão de estudo não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SessaoEstudoResponse> visualizarSessaoEstudo(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessaoId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(sessaoEstudoService.visualizarSessaoEstudo(userId, sessaoId));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Obter resumo de horas líquidas", description = "Calcula o total de horas líquidas acumuladas e contagem de sessões no período ou tópico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo de horas líquidas calculado com sucesso",
                    content = @Content(schema = @Schema(implementation = ResumoHorasLiquidasResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de período inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Tópico informado pertence a outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ResumoHorasLiquidasResponse> obterResumoHoras(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do tópico para consolidar horas específicas")
            @RequestParam(required = false) UUID topicoId,
            @Parameter(description = "Data/hora inicial em formato ISO-8601 UTC (ex: 2026-10-01T00:00:00Z)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @Parameter(description = "Data/hora final em formato ISO-8601 UTC (ex: 2026-10-07T23:59:59Z)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(sessaoEstudoService.obterResumoHoras(userId, topicoId, de, ate));
    }

    @GetMapping("/resumo/hoje")
    @Operation(summary = "Obter resumo de horas líquidas de hoje", description = "Calcula o total de horas líquidas de sessões do dia atual (resetado diariamente a cada meia-noite).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo do dia retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = ResumoHorasLiquidasResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autorizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ResumoHorasLiquidasResponse> obterResumoHoje(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identificador de fuso horário IANA (ex: America/Sao_Paulo). Se omitido, usa o fuso padrão do sistema.")
            @RequestParam(required = false) String timezone) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(sessaoEstudoService.obterResumoHoje(userId, timezone));
    }

    @DeleteMapping("/{sessaoId}")
    @Operation(summary = "Apagar sessão de estudo", description = "Exclui um registro de sessão de estudo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessão de estudo excluída com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido a sessão de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sessão de estudo não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> apagarSessaoEstudo(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessaoId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        sessaoEstudoService.apagarSessaoEstudo(userId, sessaoId);
        return ResponseEntity.noContent().build();
    }
}
