package com.kyofoundation.skillnapse.modules.desempenho.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.PainelGeralDesempenhoResponse;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.RelatorioLacunasResponse;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.ResumoMateriaDesempenhoResponse;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.TopicoCriticoResponse;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.TopicoLacunaResponse;
import com.kyofoundation.skillnapse.modules.desempenho.enums.NivelCriticidadeTopico;
import com.kyofoundation.skillnapse.modules.desempenho.mapper.DesempenhoMapper;
import com.kyofoundation.skillnapse.modules.desempenho.support.CalculoDesempenhoSupport;
import com.kyofoundation.skillnapse.modules.desempenho.validator.DesempenhoValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.MateriaRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.PlanoEstudoRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import com.kyofoundation.skillnapse.modules.questao.dto.projection.MetricasTopicoProjection;
import com.kyofoundation.skillnapse.modules.questao.finder.TentativaQuestaoFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DesempenhoService {

    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final PlanoEstudoFinder planoEstudoFinder;
    private final PlanoEstudoRepository planoEstudoRepository;
    private final MateriaRepository materiaRepository;
    private final TopicoRepository topicoRepository;
    private final TentativaQuestaoFinder tentativaQuestaoFinder;
    private final DesempenhoValidator desempenhoValidator;
    private final CalculoDesempenhoSupport calculoDesempenhoSupport;

    @Transactional(readOnly = true)
    public RelatorioLacunasResponse obterRelatorioLacunas(UUID userId, UUID planoId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        PlanoEstudo plano = planoEstudoFinder.findById(planoId);
        desempenhoValidator.validarPropriedadePlano(usuario, plano);

        List<Materia> materias = materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano);
        List<Topico> topicos = topicoRepository.findByMateriaIn(materias);

        Set<UUID> topicoIds = topicos.stream().map(Topico::getId).collect(Collectors.toSet());
        List<MetricasTopicoProjection> projections = tentativaQuestaoFinder.obterMetricasPorTopicos(usuario, topicoIds);
        Map<UUID, MetricasTopicoProjection> metricasMap = projections.stream()
                .collect(Collectors.toMap(MetricasTopicoProjection::getTopicoId, m -> m));

        Map<UUID, List<Topico>> topicosPorMateria = topicos.stream()
                .collect(Collectors.groupingBy(t -> t.getMateria().getId()));

        List<ResumoMateriaDesempenhoResponse> resumosMaterias = new ArrayList<>();
        List<TopicoLacunaResponse> todosTopicosDiagnosticados = new ArrayList<>();

        long totalQuestoesPlano = 0L;
        long totalAcertosPlano = 0L;
        int totalCriticosPlano = 0;

        for (Materia materia : materias) {
            List<Topico> topicosDaMateria = topicosPorMateria.getOrDefault(materia.getId(), List.of());
            List<TopicoLacunaResponse> topicosResponse = new ArrayList<>();

            long totalQuestoesMateria = 0L;
            long totalAcertosMateria = 0L;
            int totalCriticosMateria = 0;

            for (Topico topico : topicosDaMateria) {
                MetricasTopicoProjection metrica = metricasMap.get(topico.getId());
                long tentativas = metrica != null && metrica.getTotalTentativas() != null ? metrica.getTotalTentativas() : 0L;
                long acertos = metrica != null && metrica.getTotalAcertos() != null ? metrica.getTotalAcertos() : 0L;
                double tempoMedio = metrica != null && metrica.getTempoMedioSegundos() != null ? calculoDesempenhoSupport.arredondar(metrica.getTempoMedioSegundos()) : 0.0;
                double taxaAcerto = calculoDesempenhoSupport.calcularTaxaAcerto(tentativas, acertos);

                NivelCriticidadeTopico criticidade = calculoDesempenhoSupport.determinarCriticidade(
                        tentativas, taxaAcerto, topico.getPesoEdital(), topico.getNivelProficiencia()
                );

                if (criticidade == NivelCriticidadeTopico.CRITICO) {
                    totalCriticosMateria++;
                    totalCriticosPlano++;
                }

                totalQuestoesMateria += tentativas;
                totalAcertosMateria += acertos;

                TopicoLacunaResponse topicoResponse = DesempenhoMapper.toTopicoLacuna(
                        topico, tentativas, acertos, taxaAcerto, tempoMedio, criticidade
                );

                topicosResponse.add(topicoResponse);
                todosTopicosDiagnosticados.add(topicoResponse);
            }

            double taxaMateria = calculoDesempenhoSupport.calcularTaxaAcerto(totalQuestoesMateria, totalAcertosMateria);
            ResumoMateriaDesempenhoResponse resumoMateria = DesempenhoMapper.toResumoMateria(
                    materia, totalQuestoesMateria, totalAcertosMateria, taxaMateria, totalCriticosMateria, topicosResponse
            );

            resumosMaterias.add(resumoMateria);
            totalQuestoesPlano += totalQuestoesMateria;
            totalAcertosPlano += totalAcertosMateria;
        }

        double taxaGeralPlano = calculoDesempenhoSupport.calcularTaxaAcerto(totalQuestoesPlano, totalAcertosPlano);
        double scoreProntidao = calculoDesempenhoSupport.calcularReadinessIndex(todosTopicosDiagnosticados);

        return DesempenhoMapper.toRelatorioLacunas(
                plano, scoreProntidao, totalQuestoesPlano, totalAcertosPlano, taxaGeralPlano, totalCriticosPlano, resumosMaterias
        );
    }

    @Transactional(readOnly = true)
    public List<TopicoCriticoResponse> obterTopicosCriticos(UUID userId, UUID planoId, int limite) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        desempenhoValidator.validarLimiteTopicosCriticos(limite);

        PlanoEstudo plano = planoEstudoFinder.findById(planoId);
        desempenhoValidator.validarPropriedadePlano(usuario, plano);

        List<Materia> materias = materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano);
        List<Topico> topicos = topicoRepository.findByMateriaIn(materias);

        Set<UUID> topicoIds = topicos.stream().map(Topico::getId).collect(Collectors.toSet());
        List<MetricasTopicoProjection> projections = tentativaQuestaoFinder.obterMetricasPorTopicos(usuario, topicoIds);
        Map<UUID, MetricasTopicoProjection> metricasMap = projections.stream()
                .collect(Collectors.toMap(MetricasTopicoProjection::getTopicoId, m -> m));

        List<TopicoCriticoResponse> candidatos = new ArrayList<>();

        for (Topico topico : topicos) {
            MetricasTopicoProjection metrica = metricasMap.get(topico.getId());
            long tentativas = metrica != null && metrica.getTotalTentativas() != null ? metrica.getTotalTentativas() : 0L;
            long acertos = metrica != null && metrica.getTotalAcertos() != null ? metrica.getTotalAcertos() : 0L;
            double taxaAcerto = calculoDesempenhoSupport.calcularTaxaAcerto(tentativas, acertos);

            NivelCriticidadeTopico criticidade = calculoDesempenhoSupport.determinarCriticidade(
                    tentativas, taxaAcerto, topico.getPesoEdital(), topico.getNivelProficiencia()
            );

            double severidade = calculoDesempenhoSupport.calcularIndiceSeveridade(taxaAcerto, topico.getPesoEdital(), tentativas);

            TopicoCriticoResponse criticoResponse = DesempenhoMapper.toTopicoCritico(
                    topico.getId(),
                    topico.getTitulo(),
                    topico.getMateria().getId(),
                    topico.getMateria().getNome(),
                    topico.getPesoEdital(),
                    taxaAcerto,
                    tentativas,
                    severidade,
                    criticidade
            );

            candidatos.add(criticoResponse);
        }

        return candidatos.stream()
                .filter(t -> t.totalTentativas() > 0 && t.taxaAcerto() < 60.0)
                .sorted(Comparator
                        .comparing((TopicoCriticoResponse t) -> t.criticidade() == NivelCriticidadeTopico.CRITICO ? 0 : 1)
                        .thenComparing(Comparator.comparing(TopicoCriticoResponse::indiceSeveridade).reversed())
                        .thenComparing(Comparator.comparing(TopicoCriticoResponse::pesoEdital).reversed()))
                .limit(limite)
                .toList();
    }

    @Transactional(readOnly = true)
    public PainelGeralDesempenhoResponse obterPainelGeral(UUID userId) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        List<PlanoEstudo> planos = planoEstudoRepository.findAllByUsuario(usuario);
        List<Materia> todasMaterias = new ArrayList<>();

        for (PlanoEstudo plano : planos) {
            todasMaterias.addAll(materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano));
        }

        List<Topico> todosTopicos = topicoRepository.findByMateriaIn(todasMaterias);

        List<MetricasTopicoProjection> projections = tentativaQuestaoFinder.obterTodasMetricasPorTopicosDoUsuario(usuario);
        Map<UUID, MetricasTopicoProjection> metricasMap = projections.stream()
                .collect(Collectors.toMap(MetricasTopicoProjection::getTopicoId, m -> m));

        Map<UUID, List<Topico>> topicosPorMateria = todosTopicos.stream()
                .collect(Collectors.groupingBy(t -> t.getMateria().getId()));

        List<ResumoMateriaDesempenhoResponse> resumosMaterias = new ArrayList<>();
        List<TopicoCriticoResponse> todosTopicosCriticos = new ArrayList<>();

        long totalQuestoesGeral = 0L;
        long totalAcertosGeral = 0L;
        double somaTempoSegundosGeral = 0.0;
        int totalCriticosGeral = 0;

        for (Materia materia : todasMaterias) {
            List<Topico> topicosDaMateria = topicosPorMateria.getOrDefault(materia.getId(), List.of());
            List<TopicoLacunaResponse> topicosResponse = new ArrayList<>();

            long totalQuestoesMateria = 0L;
            long totalAcertosMateria = 0L;
            int totalCriticosMateria = 0;

            for (Topico topico : topicosDaMateria) {
                MetricasTopicoProjection metrica = metricasMap.get(topico.getId());
                long tentativas = metrica != null && metrica.getTotalTentativas() != null ? metrica.getTotalTentativas() : 0L;
                long acertos = metrica != null && metrica.getTotalAcertos() != null ? metrica.getTotalAcertos() : 0L;
                double tempoMedio = metrica != null && metrica.getTempoMedioSegundos() != null ? calculoDesempenhoSupport.arredondar(metrica.getTempoMedioSegundos()) : 0.0;
                double taxaAcerto = calculoDesempenhoSupport.calcularTaxaAcerto(tentativas, acertos);

                NivelCriticidadeTopico criticidade = calculoDesempenhoSupport.determinarCriticidade(
                        tentativas, taxaAcerto, topico.getPesoEdital(), topico.getNivelProficiencia()
                );

                if (criticidade == NivelCriticidadeTopico.CRITICO) {
                    totalCriticosMateria++;
                    totalCriticosGeral++;
                }

                totalQuestoesMateria += tentativas;
                totalAcertosMateria += acertos;
                somaTempoSegundosGeral += (tempoMedio * tentativas);

                TopicoLacunaResponse topicoResponse = DesempenhoMapper.toTopicoLacuna(
                        topico, tentativas, acertos, taxaAcerto, tempoMedio, criticidade
                );
                topicosResponse.add(topicoResponse);

                double severidade = calculoDesempenhoSupport.calcularIndiceSeveridade(taxaAcerto, topico.getPesoEdital(), tentativas);
                todosTopicosCriticos.add(DesempenhoMapper.toTopicoCritico(
                        topico.getId(),
                        topico.getTitulo(),
                        materia.getId(),
                        materia.getNome(),
                        topico.getPesoEdital(),
                        taxaAcerto,
                        tentativas,
                        severidade,
                        criticidade
                ));
            }

            double taxaMateria = calculoDesempenhoSupport.calcularTaxaAcerto(totalQuestoesMateria, totalAcertosMateria);
            resumosMaterias.add(DesempenhoMapper.toResumoMateria(
                    materia, totalQuestoesMateria, totalAcertosMateria, taxaMateria, totalCriticosMateria, topicosResponse
            ));

            totalQuestoesGeral += totalQuestoesMateria;
            totalAcertosGeral += totalAcertosMateria;
        }

        double taxaGeral = calculoDesempenhoSupport.calcularTaxaAcerto(totalQuestoesGeral, totalAcertosGeral);
        double tempoMedioGeral = totalQuestoesGeral > 0
                ? calculoDesempenhoSupport.arredondar(somaTempoSegundosGeral / totalQuestoesGeral)
                : 0.0;

        List<TopicoCriticoResponse> principaisLacunas = todosTopicosCriticos.stream()
                .sorted(Comparator
                        .comparing((TopicoCriticoResponse t) -> t.criticidade() == NivelCriticidadeTopico.CRITICO ? 0 :
                                (t.criticidade() == NivelCriticidadeTopico.ATENCAO ? 1 : 2))
                        .thenComparing(Comparator.comparing(TopicoCriticoResponse::indiceSeveridade).reversed())
                        .thenComparing(Comparator.comparing(TopicoCriticoResponse::pesoEdital).reversed()))
                .limit(5)
                .toList();

        return DesempenhoMapper.toPainelGeral(
                totalQuestoesGeral,
                totalAcertosGeral,
                taxaGeral,
                tempoMedioGeral,
                totalCriticosGeral,
                principaisLacunas,
                resumosMaterias
        );
    }
}
