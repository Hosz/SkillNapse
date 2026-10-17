package com.kyofoundation.skillnapse.modules.questao.service;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.TopicoCriticoResponse;
import com.kyofoundation.skillnapse.modules.desempenho.service.DesempenhoService;
import com.kyofoundation.skillnapse.modules.desempenho.validator.DesempenhoValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.MateriaRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.LoteQuestoesIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.QuestaoGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.request.GerarSimuladoAdaptativoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.QuestaoItemSimuladoResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.SimuladoAdaptativoResponse;
import com.kyofoundation.skillnapse.modules.questao.entity.Questao;
import com.kyofoundation.skillnapse.modules.questao.entity.Simulado;
import com.kyofoundation.skillnapse.modules.questao.mapper.SimuladoAdaptativoMapper;
import com.kyofoundation.skillnapse.modules.questao.repository.QuestaoRepository;
import com.kyofoundation.skillnapse.modules.questao.repository.SimuladoRepository;
import com.kyofoundation.skillnapse.modules.questao.support.HashEnunciadoSupport;
import com.kyofoundation.skillnapse.modules.questao.support.PromptQuestaoAdaptativaSupport;
import com.kyofoundation.skillnapse.modules.questao.validator.SimuladoAdaptativoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimuladoAdaptativoService {

    private final UserFinder userFinder;
    private final UsuarioValidator usuarioValidator;
    private final PlanoEstudoFinder planoEstudoFinder;
    private final DesempenhoValidator desempenhoValidator;
    private final SimuladoAdaptativoValidator simuladoAdaptativoValidator;
    private final TopicoFinder topicoFinder;
    private final TopicoRepository topicoRepository;
    private final MateriaRepository materiaRepository;
    private final DesempenhoService desempenhoService;
    private final AiOrchestratorService aiOrchestratorService;
    private final PromptQuestaoAdaptativaSupport promptQuestaoAdaptativaSupport;
    private final HashEnunciadoSupport hashEnunciadoSupport;
    private final QuestaoRepository questaoRepository;
    private final SimuladoRepository simuladoRepository;

    @Transactional
    public SimuladoAdaptativoResponse gerarSimuladoAdaptativo(UUID userId, GerarSimuladoAdaptativoRequest request) {
        Usuario usuario = userFinder.findById(userId);
        usuarioValidator.validarUsuarioAtivo(usuario);

        simuladoAdaptativoValidator.validarRequisicao(request);

        PlanoEstudo plano = planoEstudoFinder.findById(request.planoEstudoId());
        desempenhoValidator.validarPropriedadePlano(usuario, plano);

        int totalQuestoesDesejadas = request.quantidadeEfetiva();

        List<Topico> topicosAlvo = resolverTopicosAlvo(userId, plano, request, totalQuestoesDesejadas);
        simuladoAdaptativoValidator.validarTopicosPertencemPlano(topicosAlvo, plano);

        String tituloSimulado = gerarTituloSimulado(plano, topicosAlvo, request.bancaAlvo(), request.titulo());

        Simulado simulado = SimuladoAdaptativoMapper.toNovoSimulado(usuario, tituloSimulado);
        Simulado simuladoSalvo = simuladoRepository.save(simulado);

        List<QuestaoItemSimuladoResponse> questoesResponse = new ArrayList<>();
        int restante = totalQuestoesDesejadas;

        for (int i = 0; i < topicosAlvo.size() && restante > 0; i++) {
            Topico topico = topicosAlvo.get(i);
            int topicosRestantes = topicosAlvo.size() - i;
            int qtdParaEsteTopico = (restante + topicosRestantes - 1) / topicosRestantes;
            if (qtdParaEsteTopico <= 0) {
                qtdParaEsteTopico = 1;
            }
            if (qtdParaEsteTopico > restante) {
                qtdParaEsteTopico = restante;
            }

            int topicoRestante = qtdParaEsteTopico;
            while (topicoRestante > 0 && restante > 0) {
                int qtdBatch = Math.min(10, Math.min(topicoRestante, restante));
                String promptTexto = promptQuestaoAdaptativaSupport.construirPromptGeracao(
                        topico, qtdBatch, request.bancaAlvo()
                );

                PromptRequest promptRequest = new PromptRequest(
                        promptTexto,
                        "Você é um examinador sênior especialista na elaboração de questões de concursos públicos com saída estritamente em JSON válido.",
                        0.2,
                        8192
                );
                LoteQuestoesIaPayload lote = aiOrchestratorService.generateStructured(promptRequest, LoteQuestoesIaPayload.class);

                if (lote != null && lote.questoes() != null && !lote.questoes().isEmpty()) {
                    simuladoAdaptativoValidator.validarQuestoesGeradasIa(lote.questoes());

                    for (QuestaoGeradaIaPayload questaoPayload : lote.questoes()) {
                        if (restante <= 0 || topicoRestante <= 0) {
                            break;
                        }
                        String hash = hashEnunciadoSupport.gerarHash(questaoPayload.enunciado());
                        Questao questao = SimuladoAdaptativoMapper.toQuestaoEntity(
                                questaoPayload, topico, hash, request.bancaAlvo()
                        );
                        Questao salva = questaoRepository.save(questao);
                        questoesResponse.add(SimuladoAdaptativoMapper.toQuestaoItemSimulado(salva, topico.getId()));
                        restante--;
                        topicoRestante--;
                    }
                } else {
                    break;
                }
            }
        }

        return SimuladoAdaptativoMapper.toSimuladoAdaptativoResponse(simuladoSalvo, questoesResponse);
    }

    private List<Topico> resolverTopicosAlvo(
            UUID userId,
            PlanoEstudo plano,
            GerarSimuladoAdaptativoRequest request,
            int totalQuestoesDesejadas
    ) {
        if (request.topicoIds() != null && !request.topicoIds().isEmpty()) {
            List<Topico> topicosManuais = new ArrayList<>();
            for (UUID topicoId : request.topicoIds()) {
                topicosManuais.add(topicoFinder.findById(topicoId));
            }
            return topicosManuais;
        }

        List<TopicoCriticoResponse> criticos = desempenhoService.obterTopicosCriticos(
                userId, plano.getId(), totalQuestoesDesejadas
        );

        if (!criticos.isEmpty()) {
            List<Topico> topicosCriticos = new ArrayList<>();
            for (TopicoCriticoResponse critico : criticos) {
                topicosCriticos.add(topicoFinder.findById(critico.topicoId()));
            }
            return topicosCriticos;
        }

        List<Materia> materias = materiaRepository.findByPlanoEstudoOrderByOrdemAsc(plano);
        return topicoRepository.findByMateriaIn(materias);
    }

    private String gerarTituloSimulado(
            PlanoEstudo plano,
            List<Topico> topicosAlvo,
            String bancaAlvo,
            String tituloCustomizado
    ) {
        if (tituloCustomizado != null && !tituloCustomizado.isBlank()) {
            return truncarTitulo(tituloCustomizado.trim());
        }

        String sufixoBanca = (bancaAlvo != null && !bancaAlvo.isBlank()) ? " (" + bancaAlvo.trim() + ")" : "";

        if (topicosAlvo == null || topicosAlvo.isEmpty()) {
            return truncarTitulo("Simulado: " + plano.getTitulo() + sufixoBanca);
        }

        if (topicosAlvo.size() == 1) {
            Topico topico = topicosAlvo.get(0);
            String materiaNome = (topico.getMateria() != null && topico.getMateria().getNome() != null)
                    ? topico.getMateria().getNome()
                    : "";

            if (topico.getTopicoPai() != null) {
                String subtopico = topico.getTitulo();
                String base = materiaNome.isBlank()
                        ? subtopico
                        : materiaNome + " > " + subtopico;
                return truncarTitulo("Simulado: " + base + sufixoBanca);
            } else {
                String topicoNome = topico.getTitulo();
                String base = materiaNome.isBlank()
                        ? topicoNome
                        : materiaNome + " - " + topicoNome;
                return truncarTitulo("Simulado: " + base + sufixoBanca);
            }
        }

        Set<String> materias = topicosAlvo.stream()
                .map(t -> (t.getMateria() != null) ? t.getMateria().getNome() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (materias.size() == 1) {
            String materiaNome = materias.iterator().next();
            String base = materiaNome + " (" + topicosAlvo.size() + " tópicos)";
            return truncarTitulo("Simulado: " + base + sufixoBanca);
        }

        String base = plano.getTitulo() + " (Geral - " + topicosAlvo.size() + " tópicos)";
        return truncarTitulo("Simulado: " + base + sufixoBanca);
    }

    private String truncarTitulo(String titulo) {
        if (titulo.length() <= 300) {
            return titulo;
        }
        return titulo.substring(0, 297) + "...";
    }
}
