package com.kyofoundation.skillnapse.modules.redacao.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.AtualizarTemaRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.CriarTemaRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.GerarTemaRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResumoResponse;
import com.kyofoundation.skillnapse.modules.redacao.service.TemaRedacaoService;
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
@RequestMapping("/api/v1/redacoes/temas")
@RequiredArgsConstructor
@Tag(name = "Laboratório de Redação", description = "Endpoints para geração e consulta de propostas de redação dissertativas via IA")
@SecurityRequirement(name = "bearerAuth")
public class TemaRedacaoController {

    private final TemaRedacaoService temaRedacaoService;

    @PostMapping("/gerar")
    @Operation(summary = "Gerar tema de redação por IA", description = "Formula uma proposta inédita de redação com textos motivadores e critérios orientadores baseados no concurso e plano de estudo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tema de redação gerado com sucesso",
                    content = @Content(schema = @Schema(implementation = TemaRedacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao plano de estudo ou tópicos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plano de estudo, matéria ou tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TemaRedacaoResponse> gerar(
            @Valid @RequestBody GerarTemaRedacaoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(temaRedacaoService.gerarTema(userId, request));
    }

    @GetMapping
    @Operation(summary = "Listar temas de redação do plano", description = "Lista as propostas de redação vinculadas ao plano de estudo informado de forma paginada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listagem de temas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao plano de estudo informado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plano de estudo não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<TemaRedacaoResumoResponse>> listar(
            @RequestParam(required = false) UUID planoEstudoId,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(temaRedacaoService.listarPorPlano(userId, planoEstudoId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter tema de redação por ID", description = "Retorna os detalhes completos da proposta de redação, incluindo textos motivadores e critérios de avaliação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tema de redação encontrado",
                    content = @Content(schema = @Schema(implementation = TemaRedacaoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao tema de redação",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tema de redação não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TemaRedacaoResponse> obterPorId(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(temaRedacaoService.obterPorId(userId, id));
    }

    @PostMapping
    @Operation(summary = "Criar proposta de redação manualmente", description = "Cadastra uma proposta de redação personalizada com textos motivadores fornecidos pelo estudante.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tema de redação criado com sucesso",
                    content = @Content(schema = @Schema(implementation = TemaRedacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da proposta de redação inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao plano de estudo informado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plano de estudo não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TemaRedacaoResponse> criar(
            @Valid @RequestBody CriarTemaRedacaoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(temaRedacaoService.criarTemaManual(userId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar proposta de redação", description = "Atualiza o título, descrição, textos motivadores e critérios de uma proposta de redação existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tema de redação atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = TemaRedacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da proposta inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao tema de redação",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tema de redação não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TemaRedacaoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarTemaRedacaoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(temaRedacaoService.atualizarTema(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir proposta de redação", description = "Exclui uma proposta de redação e suas submissões associadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tema de redação excluído com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao tema de redação",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tema de redação não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> apagar(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        temaRedacaoService.apagarTema(userId, id);
        return ResponseEntity.noContent().build();
    }
}
