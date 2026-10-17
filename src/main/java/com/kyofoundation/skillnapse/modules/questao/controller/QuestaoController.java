package com.kyofoundation.skillnapse.modules.questao.controller;

import com.kyofoundation.skillnapse.common.exception.ErrorResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarQuestaoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.request.ResponderQuestaoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.HistoricoTentativaResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.QuestaoDetalheResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.QuestaoResumoResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.ResultadoResolucaoResponse;
import com.kyofoundation.skillnapse.modules.questao.enums.DificuldadeQuestao;
import com.kyofoundation.skillnapse.modules.questao.service.QuestaoService;
import com.kyofoundation.skillnapse.modules.questao.service.ResolucaoQuestaoService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questoes")
@RequiredArgsConstructor
@Tag(name = "Banco de Questões e Resoluções", description = "Endpoints para acervo de questões, busca com filtros, resolução de questões e histórico")
@SecurityRequirement(name = "bearerAuth")
public class QuestaoController {

    private final QuestaoService questaoService;
    private final ResolucaoQuestaoService resolucaoQuestaoService;

    @PostMapping
    @Operation(summary = "Cadastrar questão", description = "Cadastra uma nova questão no acervo com alternativas e gabarito comentado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Questão cadastrada com sucesso",
                    content = @Content(schema = @Schema(implementation = QuestaoDetalheResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da questão inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário inativo ou não autorizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Enunciado duplicado no acervo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<QuestaoDetalheResponse> criar(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CriarQuestaoRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(questaoService.criar(userId, request));
    }

    @GetMapping
    @Operation(summary = "Buscar questões com filtros", description = "Consulta paginada de questões por filtros dinâmicos de assunto, tópico, banca, ano, dificuldade e termo no enunciado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de questões retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário inativo ou não autorizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<QuestaoResumoResponse>> buscarComFiltros(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String assuntoGeral,
            @RequestParam(required = false) String topicoReferencia,
            @RequestParam(required = false) String banca,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) DificuldadeQuestao dificuldade,
            @RequestParam(required = false) String termoBusca,
            @ParameterObject Pageable pageable
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(questaoService.buscarComFiltros(
                userId,
                assuntoGeral,
                topicoReferencia,
                banca,
                ano,
                dificuldade,
                termoBusca,
                pageable
        ));
    }

    @GetMapping("/topico/{topicoId}")
    @Operation(summary = "Listar questões por tópico", description = "Retorna lista de questões detalhadas com todas as alternativas associadas ao tópico informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de questões detalhadas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário inativo ou não autorizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tópico não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<QuestaoDetalheResponse>> listarPorTopico(
            @PathVariable UUID topicoId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(questaoService.listarPorTopico(userId, topicoId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter questão por ID", description = "Retorna detalhes de uma questão com todas as suas alternativas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questão encontrada com sucesso",
                    content = @Content(schema = @Schema(implementation = QuestaoDetalheResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário inativo ou não autorizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Questão não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<QuestaoDetalheResponse> buscarPorId(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(questaoService.buscarPorId(userId, id));
    }

    @PostMapping("/{id}/responder")
    @Operation(summary = "Responder questão", description = "Submete a resolução de uma questão, avaliando acerto/erro e retornando gabarito comentado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questão respondida e avaliada com sucesso",
                    content = @Content(schema = @Schema(implementation = ResultadoResolucaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da resposta inválidos ou alternativa não pertencente à questão",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Simulado ou tópico pertencente a outro usuário ou usuário inativo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Questão ou alternativa não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ResultadoResolucaoResponse> responder(
            @PathVariable UUID id,
            @Valid @RequestBody ResponderQuestaoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(resolucaoQuestaoService.responder(id, request, userId));
    }

    @GetMapping("/historico")
    @Operation(summary = "Histórico de resoluções", description = "Lista as tentativas de resolução de questões do usuário autenticado de forma paginada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário inativo ou não autorizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<HistoricoTentativaResponse>> buscarHistorico(
            @RequestParam(required = false) Boolean acertou,
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(resolucaoQuestaoService.buscarHistorico(userId, acertou, pageable));
    }
}
