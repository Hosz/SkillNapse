package com.kyofoundation.skillnapse.modules.redacao.mapper;

import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.redacao.dto.payload.TemaRedacaoIaPayload;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.AtualizarTemaRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.CriarTemaRedacaoRequest;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResponse;
import com.kyofoundation.skillnapse.modules.redacao.dto.response.TemaRedacaoResumoResponse;
import com.kyofoundation.skillnapse.modules.redacao.entity.TemaRedacao;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemaRedacaoMapper {

    public static TemaRedacao toEntity(TemaRedacaoIaPayload payload, PlanoEstudo plano) {
        String tituloLimpo = lapidarTitulo(payload.titulo());
        String descricao = (payload.descricao() != null && !payload.descricao().isBlank())
                ? payload.descricao().trim()
                : null;

        return TemaRedacao.builder()
                .planoEstudo(plano)
                .titulo(tituloLimpo)
                .descricao(descricao)
                .textosMotivadores(payload.textosMotivadores().trim())
                .criteriosAvaliacao(payload.criteriosAvaliacao() != null ? payload.criteriosAvaliacao().trim() : null)
                .geradoPorIa(true)
                .build();
    }

    public static TemaRedacao toEntity(CriarTemaRedacaoRequest request, PlanoEstudo plano) {
        String descricao = (request.descricao() != null && !request.descricao().isBlank())
                ? request.descricao().trim()
                : null;
        String criterios = (request.criteriosAvaliacao() != null && !request.criteriosAvaliacao().isBlank())
                ? request.criteriosAvaliacao().trim()
                : null;

        return TemaRedacao.builder()
                .planoEstudo(plano)
                .titulo(request.titulo().trim())
                .descricao(descricao)
                .textosMotivadores(request.textosMotivadores().trim())
                .criteriosAvaliacao(criterios)
                .geradoPorIa(false)
                .build();
    }

    public static void atualizarEntity(TemaRedacao tema, AtualizarTemaRedacaoRequest request) {
        tema.setTitulo(request.titulo().trim());
        tema.setDescricao(request.descricao() != null && !request.descricao().isBlank() ? request.descricao().trim() : null);
        tema.setTextosMotivadores(request.textosMotivadores().trim());
        tema.setCriteriosAvaliacao(request.criteriosAvaliacao() != null && !request.criteriosAvaliacao().isBlank() ? request.criteriosAvaliacao().trim() : null);
    }

    public static String lapidarTitulo(String tituloBruto) {
        if (tituloBruto == null || tituloBruto.isBlank()) {
            return "Proposta de Redação Discursiva";
        }
        String t = tituloBruto.trim();

        // Se a IA tiver inserido comandos operacionais de prova no título, extrair ou limpar:
        if (t.toLowerCase().startsWith("o texto dissertativo") || t.toLowerCase().startsWith("redija um texto") || t.toLowerCase().startsWith("o candidato deve")) {
            Matcher m = Pattern.compile("(?i)(?:1\\)|tópicos?:)\\s*([^;.\n]+)").matcher(t);
            if (m.find()) {
                String extraido = m.group(1).trim();
                if (extraido.length() >= 10 && extraido.length() <= 150) {
                    return extraido;
                }
            }
            Matcher mAspas = Pattern.compile("\"([^\"]{10,130})\"").matcher(t);
            if (mAspas.find()) {
                return mAspas.group(1).trim();
            }
        }

        if (t.length() > 160) {
            int ponto = t.indexOf('.');
            if (ponto >= 10 && ponto <= 160) {
                return t.substring(0, ponto).trim();
            }
            int doisPontos = t.indexOf(':');
            if (doisPontos >= 10 && doisPontos <= 160) {
                return t.substring(0, doisPontos).trim();
            }
            return t.substring(0, 157).trim() + "...";
        }
        return t;
    }

    public static TemaRedacaoResponse toResponse(TemaRedacao tema, long totalSubmissoes) {
        return new TemaRedacaoResponse(
                tema.getId(),
                tema.getPlanoEstudo() != null ? tema.getPlanoEstudo().getId() : null,
                tema.getTitulo(),
                tema.getDescricao(),
                tema.getTextosMotivadores(),
                tema.getCriteriosAvaliacao(),
                tema.getGeradoPorIa(),
                tema.getCriadoEm(),
                totalSubmissoes
        );
    }

    public static TemaRedacaoResumoResponse toResumoResponse(TemaRedacao tema, long totalSubmissoes) {
        return new TemaRedacaoResumoResponse(
                tema.getId(),
                tema.getPlanoEstudo() != null ? tema.getPlanoEstudo().getId() : null,
                tema.getTitulo(),
                tema.getGeradoPorIa(),
                tema.getCriadoEm(),
                totalSubmissoes
        );
    }
}
