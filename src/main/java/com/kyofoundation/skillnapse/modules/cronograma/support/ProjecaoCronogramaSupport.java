package com.kyofoundation.skillnapse.modules.cronograma.support;

import com.kyofoundation.skillnapse.modules.cronograma.dto.response.AgendaDiariaResponse;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.BlocoAgendaDiariaResponse;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.TopicoRevisaoItemResponse;
import com.kyofoundation.skillnapse.modules.cronograma.entity.BlocoHorarioTemplate;
import com.kyofoundation.skillnapse.modules.cronograma.entity.ExcecaoDiaria;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import com.kyofoundation.skillnapse.modules.cronograma.enums.DiaSemana;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoAcaoExcecao;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoOrigemBloco;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProjecaoCronogramaSupport {

    public AgendaDiariaResponse projetarAgendaDoDia(
            LocalDate data,
            TemplateSemanal templateAtivo,
            List<BlocoHorarioTemplate> blocosTemplate,
            List<ExcecaoDiaria> excecoes) {

        DiaSemana diaSemana = DiaSemana.fromDayOfWeek(data.getDayOfWeek());
        UUID templateId = templateAtivo != null ? templateAtivo.getId() : null;
        String templateNome = templateAtivo != null ? templateAtivo.getNome() : null;

        Map<UUID, ExcecaoDiaria> excecoesPorBlocoOrigem = excecoes.stream()
                .filter(e -> e.getBlocoTemplateOrigem() != null)
                .collect(Collectors.toMap(e -> e.getBlocoTemplateOrigem().getId(), e -> e, (e1, e2) -> e1));

        List<BlocoAgendaDiariaResponse> blocosProjetados = new ArrayList<>();

        if (blocosTemplate != null) {
            for (BlocoHorarioTemplate bloco : blocosTemplate) {
                ExcecaoDiaria excecao = excecoesPorBlocoOrigem.get(bloco.getId());

                if (excecao == null) {
                    blocosProjetados.add(criarBlocoDoTemplate(bloco));
                } else if (excecao.getTipoAcao() == TipoAcaoExcecao.CANCELAR_BLOCO) {
                    // Bloco cancelado pontualmente no dia: não é incluído
                } else if (excecao.getTipoAcao() == TipoAcaoExcecao.SUBSTITUIR_HORARIO) {
                    blocosProjetados.add(criarBlocoSubstituido(bloco, excecao));
                }
            }
        }

        for (ExcecaoDiaria excecao : excecoes) {
            if (excecao.getTipoAcao() == TipoAcaoExcecao.BLOCO_AVULSO) {
                blocosProjetados.add(criarBlocoAvulso(excecao));
            }
        }

        blocosProjetados.sort(Comparator.comparing(BlocoAgendaDiariaResponse::horaInicio));

        return new AgendaDiariaResponse(data, diaSemana, templateId, templateNome, blocosProjetados);
    }

    private BlocoAgendaDiariaResponse criarBlocoDoTemplate(BlocoHorarioTemplate bloco) {
        UUID materiaId = bloco.getMateria() != null ? bloco.getMateria().getId() : null;
        String materiaNome = bloco.getMateria() != null ? bloco.getMateria().getNome() : null;
        UUID topicoId = bloco.getTopico() != null ? bloco.getTopico().getId() : null;
        String topicoTitulo = bloco.getTopico() != null ? bloco.getTopico().getTitulo() : null;
        List<TopicoRevisaoItemResponse> topicosRevisao = bloco.getTopicosRevisao() != null
                ? bloco.getTopicosRevisao().stream()
                        .map(t -> new TopicoRevisaoItemResponse(
                                t.getId(),
                                t.getTitulo(),
                                t.getMateria() != null ? t.getMateria().getId() : null,
                                t.getMateria() != null ? t.getMateria().getNome() : null
                        ))
                        .toList()
                : List.of();

        String titulo = bloco.getTitulo() != null ? bloco.getTitulo()
                : (topicoTitulo != null ? topicoTitulo : (materiaNome != null ? materiaNome : "Bloco de Estudo"));

        return new BlocoAgendaDiariaResponse(
                bloco.getId(),
                TipoOrigemBloco.TEMPLATE,
                null,
                bloco.getId(),
                null,
                bloco.getHoraInicio(),
                bloco.getHoraFim(),
                bloco.getTipoBloco(),
                materiaId,
                materiaNome,
                topicoId,
                topicoTitulo,
                titulo,
                topicosRevisao
        );
    }

    private BlocoAgendaDiariaResponse criarBlocoSubstituido(BlocoHorarioTemplate blocoOriginal, ExcecaoDiaria excecao) {
        LocalTime horaInicio = excecao.getHoraInicio() != null ? excecao.getHoraInicio() : blocoOriginal.getHoraInicio();
        LocalTime horaFim = excecao.getHoraFim() != null ? excecao.getHoraFim() : blocoOriginal.getHoraFim();
        TipoBloco tipoBloco = excecao.getTipoBloco() != null ? excecao.getTipoBloco() : blocoOriginal.getTipoBloco();

        UUID materiaId = excecao.getMateria() != null ? excecao.getMateria().getId()
                : (blocoOriginal.getMateria() != null ? blocoOriginal.getMateria().getId() : null);
        String materiaNome = excecao.getMateria() != null ? excecao.getMateria().getNome()
                : (blocoOriginal.getMateria() != null ? blocoOriginal.getMateria().getNome() : null);

        UUID topicoId = excecao.getTopico() != null ? excecao.getTopico().getId()
                : (blocoOriginal.getTopico() != null ? blocoOriginal.getTopico().getId() : null);
        String topicoTitulo = excecao.getTopico() != null ? excecao.getTopico().getTitulo()
                : (blocoOriginal.getTopico() != null ? blocoOriginal.getTopico().getTitulo() : null);

        List<TopicoRevisaoItemResponse> topicosRevisao = (excecao.getTopicosRevisao() != null && !excecao.getTopicosRevisao().isEmpty())
                ? excecao.getTopicosRevisao().stream()
                        .map(t -> new TopicoRevisaoItemResponse(
                                t.getId(),
                                t.getTitulo(),
                                t.getMateria() != null ? t.getMateria().getId() : null,
                                t.getMateria() != null ? t.getMateria().getNome() : null
                        ))
                        .toList()
                : (blocoOriginal.getTopicosRevisao() != null
                        ? blocoOriginal.getTopicosRevisao().stream()
                                .map(t -> new TopicoRevisaoItemResponse(
                                        t.getId(),
                                        t.getTitulo(),
                                        t.getMateria() != null ? t.getMateria().getId() : null,
                                        t.getMateria() != null ? t.getMateria().getNome() : null
                                ))
                                .toList()
                        : List.of());

        String titulo = excecao.getTitulo() != null ? excecao.getTitulo()
                : (blocoOriginal.getTitulo() != null ? blocoOriginal.getTitulo()
                : (topicoTitulo != null ? topicoTitulo : (materiaNome != null ? materiaNome : "Bloco de Estudo")));

        return new BlocoAgendaDiariaResponse(
                excecao.getId(),
                TipoOrigemBloco.EXCECAO,
                TipoAcaoExcecao.SUBSTITUIR_HORARIO,
                blocoOriginal.getId(),
                excecao.getId(),
                horaInicio,
                horaFim,
                tipoBloco,
                materiaId,
                materiaNome,
                topicoId,
                topicoTitulo,
                titulo,
                topicosRevisao
        );
    }

    private BlocoAgendaDiariaResponse criarBlocoAvulso(ExcecaoDiaria excecao) {
        UUID materiaId = excecao.getMateria() != null ? excecao.getMateria().getId() : null;
        String materiaNome = excecao.getMateria() != null ? excecao.getMateria().getNome() : null;
        UUID topicoId = excecao.getTopico() != null ? excecao.getTopico().getId() : null;
        String topicoTitulo = excecao.getTopico() != null ? excecao.getTopico().getTitulo() : null;
        List<TopicoRevisaoItemResponse> topicosRevisao = excecao.getTopicosRevisao() != null
                ? excecao.getTopicosRevisao().stream()
                        .map(t -> new TopicoRevisaoItemResponse(
                                t.getId(),
                                t.getTitulo(),
                                t.getMateria() != null ? t.getMateria().getId() : null,
                                t.getMateria() != null ? t.getMateria().getNome() : null
                        ))
                        .toList()
                : List.of();

        String titulo = excecao.getTitulo() != null ? excecao.getTitulo()
                : (topicoTitulo != null ? topicoTitulo : (materiaNome != null ? materiaNome : "Bloco Avulso"));

        return new BlocoAgendaDiariaResponse(
                excecao.getId(),
                TipoOrigemBloco.EXCECAO,
                TipoAcaoExcecao.BLOCO_AVULSO,
                null,
                excecao.getId(),
                excecao.getHoraInicio(),
                excecao.getHoraFim(),
                excecao.getTipoBloco() != null ? excecao.getTipoBloco() : TipoBloco.FOCO_TEORIA,
                materiaId,
                materiaNome,
                topicoId,
                topicoTitulo,
                titulo,
                topicosRevisao
        );
    }
}
