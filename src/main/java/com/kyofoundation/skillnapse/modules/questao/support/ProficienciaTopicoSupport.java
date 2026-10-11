package com.kyofoundation.skillnapse.modules.questao.support;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import com.kyofoundation.skillnapse.modules.questao.dto.projection.MetricasTopicoProjection;
import com.kyofoundation.skillnapse.modules.questao.entity.TentativaQuestao;
import com.kyofoundation.skillnapse.modules.questao.finder.TentativaQuestaoFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProficienciaTopicoSupport {

    private final TopicoRepository topicoRepository;
    private final TentativaQuestaoFinder tentativaQuestaoFinder;

    public NivelProficiencia calcularNivelProficiencia(long totalTentativas, long totalAcertos) {
        if (totalTentativas <= 0) {
            return NivelProficiencia.INICIANTE;
        }
        double taxa = (double) totalAcertos / totalTentativas;
        if (taxa >= 0.80) {
            return NivelProficiencia.AVANCADO;
        } else if (taxa >= 0.50) {
            return NivelProficiencia.INTERMEDIARIO;
        } else {
            return NivelProficiencia.INICIANTE;
        }
    }

    public void atualizarProficienciaTopico(Usuario usuario, Topico topico) {
        if (usuario == null || topico == null) {
            return;
        }
        List<MetricasTopicoProjection> metricas = tentativaQuestaoFinder.obterMetricasPorTopicos(usuario, List.of(topico.getId()));
        if (metricas != null && !metricas.isEmpty()) {
            MetricasTopicoProjection m = metricas.get(0);
            long total = m.getTotalTentativas() != null ? m.getTotalTentativas() : 0L;
            long acertos = m.getTotalAcertos() != null ? m.getTotalAcertos() : 0L;
            if (total > 0) {
                topico.setNivelProficiencia(calcularNivelProficiencia(total, acertos));
                topicoRepository.save(topico);
            }
        }
    }

    public void atualizarProficienciaTopicosDoSimulado(UUID simuladoId) {
        if (simuladoId == null) {
            return;
        }
        List<TentativaQuestao> tentativas = tentativaQuestaoFinder.buscarPorSimulado(simuladoId);
        if (tentativas == null || tentativas.isEmpty()) {
            return;
        }

        Map<Topico, List<TentativaQuestao>> porTopico = tentativas.stream()
                .filter(t -> t.getTopico() != null)
                .collect(Collectors.groupingBy(TentativaQuestao::getTopico));

        for (Map.Entry<Topico, List<TentativaQuestao>> entry : porTopico.entrySet()) {
            Topico topico = entry.getKey();
            List<TentativaQuestao> lista = entry.getValue();
            long totalTentativas = lista.size();
            long totalAcertos = lista.stream().filter(t -> Boolean.TRUE.equals(t.getAcertou())).count();

            if (totalTentativas > 0) {
                topico.setNivelProficiencia(calcularNivelProficiencia(totalTentativas, totalAcertos));
                topicoRepository.save(topico);
            }
        }
    }
}
