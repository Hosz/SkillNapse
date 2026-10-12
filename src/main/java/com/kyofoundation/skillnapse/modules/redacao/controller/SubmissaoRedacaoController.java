package com.kyofoundation.skillnapse.modules.redacao.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.SubmeterRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.SubmissaoRedacaoResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.SubmissaoRedacaoResumoResponse;
import com.kyofoundation.skillnapse.modules.redacao.service.SubmissaoRedacaoService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kyofoundation.skillnapse.modules.redacao.dto.response.ResultadoSubmissaoImagemResponse;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/redacoes/submissoes")
@RequiredArgsConstructor
@Tag(name = "Submissões de Redação", description = "Endpoints para submissão e consulta de redações avaliadas analiticamente por IA")
@SecurityRequirement(name = "bearerAuth")
public class SubmissaoRedacaoController {

    private final SubmissaoRedacaoService submissaoRedacaoService;

    @PostMapping
    @Operation(summary = "Submeter redação para correção analítica", description = "Avalia o texto dissertativo com base em 4 competências oficiais e gera sugestões de reescrita parágrafo a parágrafo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Redação corrigida e salva com sucesso",
                    content = @Content(schema = @Schema(implementation = SubmissaoRedacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição ou texto da redação inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao tema de redação",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tema de redação não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubmissaoRedacaoResponse> submeter(
            @Valid @RequestBody SubmeterRedacaoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(submissaoRedacaoService.submeterRedacao(userId, request));
    }

    @PostMapping(value = "/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submeter imagem de redação manuscrita", description = "Envia foto da folha de redação. A IA avalia legibilidade (mínimo 95%), transcreve e realiza a correção analítica completa (escala 0 a 1000).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Análise da imagem processada com sucesso (contendo transcrição e nota se legível >= 95%, ou recomendação de transcrição manual se ilegível)",
                    content = @Content(schema = @Schema(implementation = ResultadoSubmissaoImagemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Imagem inválida, corrompida ou fora do limite de tamanho",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao tema de redação",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tema de redação não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ResultadoSubmissaoImagemResponse> submeterImagem(
            @RequestParam("temaRedacaoId") UUID temaRedacaoId,
            @RequestParam("imagem") MultipartFile imagem,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(submissaoRedacaoService.submeterRedacaoImagem(userId, temaRedacaoId, imagem));
    }

    @GetMapping
    @Operation(summary = "Listar redações do usuário", description = "Lista as redações submetidas de forma paginada, permitindo filtro opcional por tema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listagem de redações retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao tema informado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tema informado não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<SubmissaoRedacaoResumoResponse>> listar(
            @RequestParam(required = false) UUID temaRedacaoId,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(submissaoRedacaoService.listar(userId, temaRedacaoId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes e feedback de uma redação", description = "Retorna o parecer analítico completo, pontuação por competência e sugestões de reescrita.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Redação encontrada com sucesso",
                    content = @Content(schema = @Schema(implementation = SubmissaoRedacaoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado à redação de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Submissão de redação não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubmissaoRedacaoResponse> obterPorId(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(submissaoRedacaoService.obterPorId(userId, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir submissão de redação", description = "Remove uma redação avaliada do histórico do estudante.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Submissão de redação excluída com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado à submissão de redação",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Submissão de redação não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> apagar(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        submissaoRedacaoService.apagarSubmissao(userId, id);
        return ResponseEntity.noContent().build();
    }
}
