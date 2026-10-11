package com.kyofoundation.skillnapse.modules.cronograma.support;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.AplicarAutoAgendamentoRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.BlocoParaAplicarRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.BlocoSugeridoResponse;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.ResultadoAutoAgendamentoResponse;
import com.kyofoundation.skillnapse.modules.cronograma.entity.BlocoHorarioTemplate;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import com.kyofoundation.skillnapse.modules.cronograma.finder.TemplateSemanalFinder;
import com.kyofoundation.skillnapse.modules.cronograma.mapper.AutoAgendamentoMapper;
import com.kyofoundation.skillnapse.modules.cronograma.repository.BlocoHorarioTemplateRepository;
import com.kyofoundation.skillnapse.modules.cronograma.repository.TemplateSemanalRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AutoAgendamentoPersistenciaSupport {

    private final TemplateSemanalRepository templateSemanalRepository;
    private final BlocoHorarioTemplateRepository blocoHorarioTemplateRepository;
    private final TemplateSemanalFinder templateSemanalFinder;

    public ResultadoAutoAgendamentoResponse persistirBlocosAprovados(
            Usuario usuario,
            AplicarAutoAgendamentoRequest request,
            Map<UUID, Materia> mapaMaterias,
            Map<UUID, Topico> mapaTopicos) {

        TemplateSemanal template;
        if (request.templateSemanalId() != null) {
            template = templateSemanalFinder.findById(request.templateSemanalId());
            if (request.limparBlocosExistentesEfetivo() && template.getBlocosHorario() != null) {
                blocoHorarioTemplateRepository.deleteAll(template.getBlocosHorario());
                template.getBlocosHorario().clear();
            }
        } else {
            String nome = request.nomeTemplate() != null && !request.nomeTemplate().isBlank()
                    ? request.nomeTemplate().trim()
                    : "Auto-Agendamento IA";

            List<TemplateSemanal> ativosExistentes = templateSemanalRepository.findAllByUsuarioAndAtivoTrue(usuario);
            for (TemplateSemanal ativoExistente : ativosExistentes) {
                ativoExistente.setAtivo(false);
            }
            if (!ativosExistentes.isEmpty()) {
                templateSemanalRepository.saveAll(ativosExistentes);
            }

            template = TemplateSemanal.builder()
                    .usuario(usuario)
                    .nome(nome)
                    .ativo(true)
                    .build();
            template = templateSemanalRepository.save(template);
        }

        List<BlocoSugeridoResponse> blocosSalvos = new ArrayList<>();

        if (request.blocos() != null) {
            for (BlocoParaAplicarRequest blocoRequest : request.blocos()) {
                Materia materia = blocoRequest.materiaId() != null ? mapaMaterias.get(blocoRequest.materiaId()) : null;
                Topico topico = blocoRequest.topicoId() != null ? mapaTopicos.get(blocoRequest.topicoId()) : null;
                TipoBloco tipoBloco = blocoRequest.tipoBloco() != null ? blocoRequest.tipoBloco() : TipoBloco.FOCO_TEORIA;

                String tituloBloco = topico != null ? topico.getTitulo() : (materia != null ? materia.getNome() : "Bloco de Estudo");

                BlocoHorarioTemplate bloco = BlocoHorarioTemplate.builder()
                        .templateSemanal(template)
                        .diaSemana(blocoRequest.diaSemana())
                        .horaInicio(blocoRequest.horaInicio())
                        .horaFim(blocoRequest.horaFim())
                        .tipoBloco(tipoBloco)
                        .titulo(tituloBloco)
                        .materia(materia)
                        .topico(topico)
                        .build();

                blocoHorarioTemplateRepository.save(bloco);

                blocosSalvos.add(new BlocoSugeridoResponse(
                        blocoRequest.diaSemana(),
                        blocoRequest.horaInicio(),
                        blocoRequest.horaFim(),
                        tipoBloco,
                        materia != null ? materia.getId() : null,
                        materia != null ? materia.getNome() : null,
                        topico != null ? topico.getId() : null,
                        topico != null ? topico.getTitulo() : null,
                        blocoRequest.justificativaPedagogica()
                ));
            }
        }

        String resumo = request.resumoPedagogico() != null && !request.resumoPedagogico().isBlank()
                ? request.resumoPedagogico()
                : "Grade pedagógica semanal aplicada com sucesso.";

        return AutoAgendamentoMapper.toResultadoResponse(
                template,
                blocosSalvos.size(),
                resumo,
                blocosSalvos
        );
    }
}
