package com.kyofoundation.skillnapse.modules.cronograma.support;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.GerarAutoAgendamentoRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.JanelaDisponibilidadeRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AutoAgendamentoPromptSupport {

    private static final String SYSTEM_PROMPT = """
            Você é o motor pedagógico de auto-agendamento e distribuição inteligente de estudos do ecossistema SkillNapse.
            Sua missão é gerar uma grade semanal de blocos de estudo altamente eficiente e pedagogicamente equilibrada.
            
            Regras obrigatórias de agendamento:
            1. Respeite RIGOROSAMENTE as janelas de horários disponíveis informadas para cada dia da semana. Nenhum bloco pode iniciar antes ou terminar depois da janela disponível.
            2. Cada bloco de estudo deve ter a duração configurada (em minutos). Respeite os intervalos de descanso entre blocos dentro da janela.
            3. Não gere blocos conflitantes ou com horários sobrepostos no mesmo dia da semana.
            4. Prática Intercalada (Interleaving): evite blocos consecutivos da mesma matéria. Alterne disciplinas teóricas e práticas para otimizar a retenção e diminuir a fadiga cognitiva.
            5. Priorização por Peso e Proficiência: matérias e tópicos com maior 'pesoEdital' (7 a 10) ou menor proficiência ('INICIANTE') devem ter prioridade na alocação de tempo conforme a estratégia informada.
            6. Inclusão de Tipos de Bloco:
               - Use 'FOCO_TEORIA' para aprendizado de tópicos.
               - Se solicitado 'incluirRevisao', adicione blocos do tipo 'REVISAO'.
               - Se solicitado 'incluirSimulado', aloque blocos do tipo 'SIMULADO' (preferencialmente nos fins de semana ou final de ciclo).
            7. Mapeamento de IDs: utilize EXATAMENTE os UUIDs das matérias e tópicos informados no catálogo. Para blocos de SIMULADO geral ou DESCANSO, materiaId e topicoId podem ser nulos.
            8. Justificativa Pedagógica: forneça em cada bloco uma breve razão pedagógica (ex: "Fixação inicial de Direito Constitucional devido ao peso 5 no edital").
            """;

    public PromptRequest criarPrompt(
            PlanoEstudo plano,
            List<Materia> materias,
            List<Topico> topicos,
            GerarAutoAgendamentoRequest request) {

        String janelasTexto = formatarJanelas(request.disponibilidades());
        String catalogoConteudo = formatarCatalogoMateriasETopicos(materias, topicos);

        String instrucoesTexto = (request.instrucoesPersonalizadas() != null && !request.instrucoesPersonalizadas().isBlank())
                ? String.format("""

                --- SITUAÇÃO ESPECÍFICA E INSTRUÇÕES PERSONALIZADAS DO ESTUDANTE ---
                %s
                DIRETRIZ PEDAGÓGICA: Leve em consideração máxima e prioritária a rotina, particularidades de horário/trabalho, facilidades e dificuldades descritas pelo estudante ao distribuir as matérias na grade.
                """, request.instrucoesPersonalizadas().trim())
                : "";

        String userPrompt = String.format("""
                Plano de Estudo: "%s" (%s)
                Estratégia Pedagógica Solicitada: %s
                Duração de cada Bloco: %d minutos
                Intervalo de Descanso entre Blocos: %d minutos
                Incluir Revisões: %s
                Incluir Simulados: %s
                %s
                --- JANELAS DE DISPONIBILIDADE SEMANAL ---
                %s

                --- MATÉRIAS E TÓPICOS DISPONÍVEIS ---
                %s

                Gere a distribuição semanal dos blocos de horário preenchendo todos os campos da estrutura.
                """,
                plano.getTitulo(),
                plano.getDescricao() != null ? plano.getDescricao() : "Sem descrição",
                request.estrategiaEfetiva(),
                request.duracaoBlocoMinutosEfetiva(),
                request.intervaloDescansoMinutosEfetivo(),
                request.incluirRevisaoEfetivo() ? "Sim" : "Não",
                request.incluirSimuladoEfetivo() ? "Sim" : "Não",
                instrucoesTexto,
                janelasTexto,
                catalogoConteudo
        );

        return new PromptRequest(
                userPrompt,
                SYSTEM_PROMPT,
                0.2,
                4000
        );
    }

    private String formatarJanelas(List<JanelaDisponibilidadeRequest> janelas) {
        return janelas.stream()
                .sorted(Comparator.comparing(JanelaDisponibilidadeRequest::diaSemana)
                        .thenComparing(JanelaDisponibilidadeRequest::horaInicio))
                .map(j -> String.format("- %s: das %s às %s", j.diaSemana(), j.horaInicio(), j.horaFim()))
                .collect(Collectors.joining("\n"));
    }

    private String formatarCatalogoMateriasETopicos(List<Materia> materias, List<Topico> topicos) {
        Map<Materia, List<Topico>> topicosPorMateria = topicos.stream()
                .collect(Collectors.groupingBy(Topico::getMateria));

        StringBuilder sb = new StringBuilder();
        for (Materia m : materias) {
            sb.append(String.format("Matéria ID: %s | Nome: \"%s\"\n", m.getId(), m.getNome()));
            List<Topico> listaTopicos = topicosPorMateria.getOrDefault(m, List.of());
            if (listaTopicos.isEmpty()) {
                sb.append("   (Sem tópicos específicos cadastrados)\n");
            } else {
                for (Topico t : listaTopicos) {
                    sb.append(String.format("   - Tópico ID: %s | Título: \"%s\" | Peso: %d | Proficiência: %s\n",
                            t.getId(), t.getTitulo(), t.getPesoEdital(), t.getNivelProficiencia()));
                }
            }
        }
        return sb.toString();
    }
}
