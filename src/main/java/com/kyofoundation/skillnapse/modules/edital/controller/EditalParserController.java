package com.kyofoundation.skillnapse.modules.edital.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.edital.dto.request.AtualizarRascunhoEditalRequest;
import com.kyofoundation.skillnapse.modules.edital.dto.request.ConverterRascunhoEditalRequest;
import com.kyofoundation.skillnapse.modules.edital.dto.response.ConversaoEditalResponse;
import com.kyofoundation.skillnapse.modules.edital.dto.response.RascunhoEditalResponse;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalCargosDetectados;
import com.kyofoundation.skillnapse.modules.edital.service.EditalParserService;
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
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/editais")
@RequiredArgsConstructor
@Tag(name = "Editais (Processamento de PDF e IA)", description = "Endpoints para ingestão de editais em PDF e conversão em planos de estudo")
@SecurityRequirement(name = "bearerAuth")
public class EditalParserController {

    private final EditalParserService editalParserService;

    @PostMapping(value = "/detectar-cargos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Detectar cargos disponíveis no edital", description = "Analisa o PDF do edital via IA e identifica todos os cargos ou especialidades disponíveis para seleção do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cargos identificados com sucesso",
                    content = @Content(schema = @Schema(implementation = EditalCargosDetectados.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido, vazio ou não compatível",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EditalCargosDetectados> detectarCargos(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Arquivo PDF do edital a ser analisado (máx 25MB)")
            @RequestParam("arquivo") MultipartFile arquivo) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(editalParserService.detectarCargos(userId, arquivo));
    }

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload e parsing de edital em PDF com Raio-X da Banca",
            description = "Extrai o conteúdo programático do edital em PDF e cria um rascunho com a árvore estruturada via IA. " +
                    "Realiza pesquisa web e análise estatística de provas anteriores da banca organizadora (últimos 3 a 5 anos), " +
                    "calibrando o peso dos tópicos e fornecendo justificativas transparentes. Por envolver pesquisa e processamento de IA, " +
                    "a operação pode levar de 15 a 35 segundos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Edital processado com sucesso e rascunho criado",
                    content = @Content(schema = @Schema(implementation = RascunhoEditalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido, vazio ou não compatível",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RascunhoEditalResponse> uploadEditalPdf(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Arquivo PDF do edital a ser processado (máx 25MB)")
            @RequestParam("arquivo") MultipartFile arquivo,
            @Parameter(description = "Cargo alvo desejado para filtrar e estruturar o conteúdo programático (opcional)")
            @RequestParam(value = "cargoAlvo", required = false) String cargoAlvo) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(editalParserService.uploadEditalPdf(userId, arquivo, cargoAlvo));
    }

    @GetMapping("/rascunhos")
    @Operation(summary = "Listar rascunhos de editais", description = "Retorna os rascunhos de editais pertencentes ao usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de rascunhos retornada com sucesso")
    })
    public ResponseEntity<Page<RascunhoEditalResponse>> listarRascunhos(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @ParameterObject Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(editalParserService.listarRascunhos(userId, pageable));
    }

    @GetMapping("/rascunhos/{rascunhoId}")
    @Operation(summary = "Visualizar rascunho de edital", description = "Retorna os detalhes e a árvore programática de um rascunho específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rascunho encontrado com sucesso",
                    content = @Content(schema = @Schema(implementation = RascunhoEditalResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido a rascunho de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rascunho não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RascunhoEditalResponse> visualizarRascunho(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID rascunhoId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(editalParserService.visualizarRascunho(userId, rascunhoId));
    }

    @PutMapping("/rascunhos/{rascunhoId}")
    @Operation(summary = "Atualizar rascunho de edital", description = "Atualiza a árvore hierárquica do rascunho antes da conversão final em plano de estudo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rascunho atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = RascunhoEditalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Rascunho não pode ser editado (já convertido)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido a rascunho de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rascunho não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RascunhoEditalResponse> atualizarRascunho(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID rascunhoId,
            @Valid @RequestBody AtualizarRascunhoEditalRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(editalParserService.atualizarRascunho(userId, rascunhoId, request));
    }

    @PostMapping("/rascunhos/{rascunhoId}/converter")
    @Operation(summary = "Converter rascunho em plano de estudo", description = "Gera o plano de estudo oficial e persiste todas as matérias e tópicos da árvore no banco relacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano de estudo criado com sucesso a partir do edital",
                    content = @Content(schema = @Schema(implementation = ConversaoEditalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Rascunho já convertido ou dados inconsistentes",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido a rascunho de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rascunho não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConversaoEditalResponse> converterEmPlanoEstudo(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID rascunhoId,
            @Valid @RequestBody ConverterRascunhoEditalRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(editalParserService.converterEmPlanoEstudo(userId, rascunhoId, request));
    }

    @DeleteMapping("/rascunhos/{rascunhoId}")
    @Operation(summary = "Excluir rascunho de edital", description = "Remove um rascunho de edital descartado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rascunho removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido a rascunho de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rascunho não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> excluirRascunho(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID rascunhoId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        editalParserService.excluirRascunho(userId, rascunhoId);
        return ResponseEntity.noContent().build();
    }
}
