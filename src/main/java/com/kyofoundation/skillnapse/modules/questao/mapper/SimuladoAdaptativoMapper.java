package com.kyofoundation.skillnapse.modules.questao.mapper;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.AlternativaGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.QuestaoGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.response.AlternativaItemSimuladoResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.QuestaoItemSimuladoResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.SimuladoAdaptativoResponse;
import com.kyofoundation.skillnapse.modules.questao.entity.AlternativaQuestao;
import com.kyofoundation.skillnapse.modules.questao.entity.Questao;
import com.kyofoundation.skillnapse.modules.questao.entity.Simulado;
import com.kyofoundation.skillnapse.modules.questao.enums.DificuldadeQuestao;
import com.kyofoundation.skillnapse.modules.questao.enums.TipoSimulado;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SimuladoAdaptativoMapper {

    private static final String[] LETRAS_ALTERNATIVAS = {"A", "B", "C", "D", "E"};

    public static Simulado toNovoSimulado(Usuario usuario, String titulo) {
        return Simulado.builder()
                .usuario(usuario)
                .titulo(titulo != null && !titulo.isBlank() ? titulo.trim() : "Simulado Adaptativo IA")
                .tipo(TipoSimulado.ADAPTATIVO_IA)
                .concluido(false)
                .tentativas(new ArrayList<>())
                .build();
    }

    public static Questao toQuestaoEntity(
            QuestaoGeradaIaPayload payload,
            Topico topico,
            String hashEnunciado,
            String bancaAlvo
    ) {
        String assunto = (topico != null && topico.getMateria() != null && topico.getMateria().getNome() != null)
                ? topico.getMateria().getNome()
                : "Geral";
        String referencia = (topico != null && topico.getTitulo() != null)
                ? topico.getTitulo()
                : "Tópico Geral";

        DificuldadeQuestao dificuldade = DificuldadeQuestao.MEDIA;
        if (payload.dificuldade() != null) {
            try {
                dificuldade = DificuldadeQuestao.valueOf(payload.dificuldade().toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {
                // Mantém MEDIA se a IA responder string variante
            }
        }

        Questao questao = Questao.builder()
                .assuntoGeral(assunto)
                .topicoReferencia(referencia)
                .enunciado(payload.enunciado().trim())
                .hashEnunciado(hashEnunciado)
                .explicacaoGabarito(payload.explicacaoGabarito() != null ? payload.explicacaoGabarito().trim() : "")
                .dificuldade(dificuldade)
                .banca(bancaAlvo != null && !bancaAlvo.isBlank() ? bancaAlvo.trim() : "IA - Adaptativo")
                .ano(LocalDate.now().getYear())
                .geradaPorIa(true)
                .alternativas(new ArrayList<>())
                .build();

        if (payload.alternativas() != null) {
            for (int i = 0; i < payload.alternativas().size(); i++) {
                AlternativaGeradaIaPayload altPayload = payload.alternativas().get(i);
                String letra = (i < LETRAS_ALTERNATIVAS.length) ? LETRAS_ALTERNATIVAS[i] : String.valueOf((char) ('A' + i));

                AlternativaQuestao alternativa = AlternativaQuestao.builder()
                        .questao(questao)
                        .letra(letra)
                        .texto(altPayload.texto() != null ? altPayload.texto().trim() : "")
                        .correta(Boolean.TRUE.equals(altPayload.correta()))
                        .build();

                questao.getAlternativas().add(alternativa);
            }
        }

        return questao;
    }

    public static QuestaoItemSimuladoResponse toQuestaoItemSimulado(Questao questao, UUID topicoId) {
        List<AlternativaItemSimuladoResponse> alternativas = new ArrayList<>();
        if (questao.getAlternativas() != null) {
            for (AlternativaQuestao alt : questao.getAlternativas()) {
                alternativas.add(new AlternativaItemSimuladoResponse(
                        alt.getId(),
                        alt.getLetra(),
                        alt.getTexto()
                ));
            }
        }

        return new QuestaoItemSimuladoResponse(
                questao.getId(),
                topicoId,
                questao.getAssuntoGeral(),
                questao.getTopicoReferencia(),
                questao.getEnunciado(),
                questao.getDificuldade(),
                questao.getBanca(),
                questao.getAno(),
                alternativas
        );
    }

    public static QuestaoItemSimuladoResponse toQuestaoItemSimuladoComGabarito(
            Questao questao,
            UUID topicoId,
            com.kyofoundation.skillnapse.modules.questao.entity.TentativaQuestao tentativa
    ) {
        List<AlternativaItemSimuladoResponse> alternativas = new ArrayList<>();
        UUID altCorretaId = null;
        String letraCorreta = null;

        if (questao.getAlternativas() != null) {
            for (AlternativaQuestao alt : questao.getAlternativas()) {
                boolean isCorreta = Boolean.TRUE.equals(alt.getCorreta());
                if (isCorreta) {
                    altCorretaId = alt.getId();
                    letraCorreta = alt.getLetra();
                }
                alternativas.add(new AlternativaItemSimuladoResponse(
                        alt.getId(),
                        alt.getLetra(),
                        alt.getTexto(),
                        isCorreta
                ));
            }
        }

        UUID altEscolhidaId = (tentativa != null && tentativa.getAlternativaEscolhida() != null)
                ? tentativa.getAlternativaEscolhida().getId()
                : null;
        String letraEscolhida = (tentativa != null && tentativa.getAlternativaEscolhida() != null)
                ? tentativa.getAlternativaEscolhida().getLetra()
                : null;
        Boolean acertou = (tentativa != null) ? tentativa.getAcertou() : null;

        return new QuestaoItemSimuladoResponse(
                questao.getId(),
                topicoId,
                questao.getAssuntoGeral(),
                questao.getTopicoReferencia(),
                questao.getEnunciado(),
                questao.getDificuldade(),
                questao.getBanca(),
                questao.getAno(),
                alternativas,
                altCorretaId,
                letraCorreta,
                altEscolhidaId,
                letraEscolhida,
                acertou,
                questao.getExplicacaoGabarito()
        );
    }

    public static SimuladoAdaptativoResponse toSimuladoAdaptativoResponse(
            Simulado simulado,
            List<QuestaoItemSimuladoResponse> questoes
    ) {
        return new SimuladoAdaptativoResponse(
                simulado.getId(),
                simulado.getTitulo(),
                simulado.getTipo(),
                simulado.getConcluido(),
                simulado.getCriadoEm() != null ? simulado.getCriadoEm() : Instant.now(),
                questoes != null ? questoes.size() : 0,
                questoes != null ? questoes : List.of()
        );
    }
}
