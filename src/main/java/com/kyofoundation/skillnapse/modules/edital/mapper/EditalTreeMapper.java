package com.kyofoundation.skillnapse.modules.edital.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.edital.dto.response.RascunhoEditalResponse;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalArvoreEstruturada;
import com.kyofoundation.skillnapse.modules.edital.entity.RascunhoEdital;
import com.kyofoundation.skillnapse.modules.edital.enums.StatusRascunhoEdital;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class EditalTreeMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static String toJson(EditalArvoreEstruturada arvoreEstruturada) {
        if (arvoreEstruturada == null) {
            throw new BadRequestException("O conteúdo da árvore do edital não pode ser nulo.");
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(arvoreEstruturada);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Falha ao serializar a árvore do edital para formato JSON: " + e.getMessage());
        }
    }

    public static EditalArvoreEstruturada fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, EditalArvoreEstruturada.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Falha ao converter o JSON do edital em árvore de matérias e tópicos.");
        }
    }

    public static RascunhoEdital toEntity(String originalFilename, String jsonString, Usuario usuario) {
        return RascunhoEdital.builder()
                .usuario(usuario)
                .nomeArquivo(originalFilename)
                .conteudoExtraidoJson(jsonString)
                .status(StatusRascunhoEdital.AGUARDANDO_APROVACAO)
                .build();
    }

    public static RascunhoEditalResponse toResponse(RascunhoEdital rascunho, EditalArvoreEstruturada arvoreEstruturada) {
        if (rascunho == null) {
            return null;
        }
        UUID planoEstudoId = rascunho.getPlanoEstudo() != null ? rascunho.getPlanoEstudo().getId() : null;
        return new RascunhoEditalResponse(
                rascunho.getId(),
                rascunho.getNomeArquivo(),
                planoEstudoId,
                rascunho.getStatus(),
                arvoreEstruturada,
                rascunho.getCriadoEm()
        );
    }

    public static RascunhoEditalResponse toResponse(RascunhoEdital rascunho) {
        if (rascunho == null) {
            return null;
        }
        EditalArvoreEstruturada arvore = fromJson(rascunho.getConteudoExtraidoJson());
        return toResponse(rascunho, arvore);
    }

    public static RascunhoEditalResponse toResponseAguardandoSelecaoCargo(
            String originalFilename,
            String nomeConcurso,
            List<String> cargosDisponiveis
    ) {
        EditalArvoreEstruturada arvoreInicial = new EditalArvoreEstruturada(
                nomeConcurso != null && !nomeConcurso.isBlank() ? nomeConcurso : "Concurso Público",
                "",
                List.of()
        );
        return new RascunhoEditalResponse(
                null,
                originalFilename,
                null,
                StatusRascunhoEdital.PROCESSANDO,
                arvoreInicial,
                cargosDisponiveis != null ? cargosDisponiveis : List.of(),
                true,
                Instant.now()
        );
    }
}
