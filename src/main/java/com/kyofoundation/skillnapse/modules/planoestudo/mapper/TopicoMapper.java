package com.kyofoundation.skillnapse.modules.planoestudo.mapper;

import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.AtualizarProgressoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EditarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.TopicoResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TopicoMapper {

    public static Topico toCriarTopico(Materia materia, Topico topicoPai, CriarTopicoRequest request) {
        Integer ordem = (request.ordem() != null) ? request.ordem() : 0;
        Integer peso = (request.pesoEdital() != null) ? request.pesoEdital() : 1;
        NivelProficiencia nivel = (request.nivelProficiencia() != null) ? request.nivelProficiencia() : NivelProficiencia.INICIANTE;

        return Topico.builder()
                .materia(materia)
                .topicoPai(topicoPai)
                .titulo(request.titulo().trim())
                .nivelProficiencia(nivel)
                .pesoEdital(peso)
                .concluido(false)
                .ordem(ordem)
                .build();
    }

    public static TopicoResponse toResponse(Topico topico) {

        UUID topicoPaiId = (topico.getTopicoPai() != null) ? topico.getTopicoPai().getId() : null;
        UUID materiaId = (topico.getMateria() != null) ? topico.getMateria().getId() : null;

        List<TopicoResponse> subtopicos = (topico.getSubtopicos() == null || topico.getSubtopicos().isEmpty())
                ? List.of()
                : topico.getSubtopicos().stream()
                        .sorted((a, b) -> {
                            int comp = Integer.compare(
                                    a.getOrdem() != null ? a.getOrdem() : 0,
                                    b.getOrdem() != null ? b.getOrdem() : 0
                            );
                            if (comp != 0) return comp;
                            if (a.getCriadoEm() != null && b.getCriadoEm() != null) {
                                return a.getCriadoEm().compareTo(b.getCriadoEm());
                            }
                            return 0;
                        })
                        .map(TopicoMapper::toResponse)
                        .toList();

        return new TopicoResponse(
                topico.getId(),
                materiaId,
                topicoPaiId,
                topico.getTitulo(),
                topico.getNivelProficiencia(),
                topico.getPesoEdital(),
                topico.getConcluido(),
                topico.getOrdem(),
                topico.getCriadoEm(),
                topico.getAtualizadoEm(),
                subtopicos
        );
    }

    public static void toEditarTopico(Topico topico, EditarTopicoRequest request) {
        if (request.titulo() != null && !request.titulo().isBlank()) {
            topico.setTitulo(request.titulo().trim());
        }
        if (request.pesoEdital() != null) {
            topico.setPesoEdital(request.pesoEdital());
        }
        if (request.nivelProficiencia() != null) {
            topico.setNivelProficiencia(request.nivelProficiencia());
        }
        if (request.concluido() != null) {
            topico.setConcluido(request.concluido());
        }
        if (request.ordem() != null) {
            topico.setOrdem(request.ordem());
        }
    }

    public static void toAtualizarProgresso(Topico topico, AtualizarProgressoRequest request) {
        if (request.concluido() != null) {
            topico.setConcluido(request.concluido());
        }
        if (request.nivelProficiencia() != null) {
            topico.setNivelProficiencia(request.nivelProficiencia());
        }
    }
}
