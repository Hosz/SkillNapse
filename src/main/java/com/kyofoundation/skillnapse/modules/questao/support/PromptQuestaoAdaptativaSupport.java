package com.kyofoundation.skillnapse.modules.questao.support;

import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import org.springframework.stereotype.Component;

@Component
public class PromptQuestaoAdaptativaSupport {

    public String construirPromptGeracao(
            Topico topico,
            int quantidadeQuestoes,
            String bancaAlvo
    ) {
        String materiaNome = (topico.getMateria() != null && topico.getMateria().getNome() != null)
                ? topico.getMateria().getNome()
                : "Geral";
        String topicoNome = topico.getTitulo() != null ? topico.getTitulo() : "Tópico Geral";
        String banca = (bancaAlvo != null && !bancaAlvo.isBlank()) ? bancaAlvo.trim() : "Bancas Tradicionais de Concurso (FGV/FCC/Cebraspe)";
        String proficiencia = topico.getNivelProficiencia() != null ? topico.getNivelProficiencia().name() : "INTERMEDIARIO";

        return """
                Você é um examinador sênior especialista na elaboração de questões de alto nível para concursos públicos e exames acadêmicos de ponta.
                
                Sua tarefa é elaborar exatamente %d questão(ões) inédita(s) de múltipla escolha focada(s) estritamente no seguinte tópico:
                - Matéria: %s
                - Tópico Específico: %s
                - Nível de Proficiência Atual do Estudante: %s
                - Estilo/Banca de Referência: %s
                
                REQUISITOS ESTRITOS DE QUALIDADE:
                1. O enunciado deve ser contextualizado, técnico e sem ambiguidades, apresentando uma situação-problema ou caso prático relevante.
                2. Cada questão deve possuir exatamente entre 4 e 5 alternativas (ordem 1 a 4 ou 1 a 5).
                3. Exatamente UMA alternativa deve ter 'correta: true'. Todas as outras devem ter 'correta: false'.
                4. Crie distratores plausíveis baseados em confusões conceituais comuns, evitando alternativas absurdas ou pegadinhas de dupla negação vazia.
                5. O campo 'explicacaoGabarito' deve conter a fundamentação teórica detalhada explicando por que a alternativa correta é a válida e por que os distratores estão incorretos.
                6. O campo 'dificuldade' deve ser um dos valores: 'FACIL', 'MEDIA' ou 'DIFICIL'.
                
                DIRETRIZES DE FORMATAÇÃO E SINTAXE JSON OBRIGATÓRIAS:
                - Retorne EXCLUSIVAMENTE um objeto JSON válido no formato do schema abaixo, sem texto antes ou depois.
                - NUNCA use aspas duplas desprotegidas dentro de strings (enunciado, texto, explicacaoGabarito). Caso precise citar termos, artigos ou trechos de lei, utilize aspas simples 'exemplo' ou escape as aspas duplas (\\"exemplo\\").
                - Todos os campos e itens de array DEVEM ser obrigatoriamente separados por vírgulas válidas.
                - O nome exato da propriedade de explicação da resposta é 'explicacaoGabarito'.
                
                SCHEMA JSON ESPERADO:
                {
                  "questoes": [
                    {
                      "enunciado": "Texto do enunciado contextualizado...",
                      "dificuldade": "MEDIA",
                      "explicacaoGabarito": "Explicação fundamentada do gabarito...",
                      "alternativas": [
                        { "ordem": 1, "texto": "Texto da alternativa A", "correta": false },
                        { "ordem": 2, "texto": "Texto da alternativa B", "correta": true },
                        { "ordem": 3, "texto": "Texto da alternativa C", "correta": false },
                        { "ordem": 4, "texto": "Texto da alternativa D", "correta": false }
                      ]
                    }
                  ]
                }
                """.formatted(quantidadeQuestoes, materiaNome, topicoNome, proficiencia, banca);
    }
}
