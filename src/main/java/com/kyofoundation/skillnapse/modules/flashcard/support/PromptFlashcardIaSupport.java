package com.kyofoundation.skillnapse.modules.flashcard.support;

import org.springframework.stereotype.Component;

@Component
public class PromptFlashcardIaSupport {

    public String construirPrompt(
            String nomePlano,
            String nomeMateria,
            String nomeTopico,
            String nomeSubtopico,
            int quantidade
    ) {
        String contextoPlano = (nomePlano != null && !nomePlano.isBlank()) ? nomePlano.trim() : "Concurso Geral";
        String contextoMateria = (nomeMateria != null && !nomeMateria.isBlank()) ? nomeMateria.trim() : "Disciplinas Gerais";
        String contextoTopico = (nomeTopico != null && !nomeTopico.isBlank()) ? nomeTopico.trim() : "Conteúdo Geral";
        String contextoSubtopico = (nomeSubtopico != null && !nomeSubtopico.isBlank())
                ? String.format("- Subtópico Específico Prioritário: %s\n", nomeSubtopico.trim())
                : "";

        return String.format("""
                Você é um especialista sênior em neurociência da aprendizagem, repetição espaçada (SRS) e memorização ativa para provas e concursos públicos.
                
                Sua missão é gerar exatamente %d flashcards de alto rendimento e memorização ativa baseados no seguinte escopo pedagógico:
                - Edital / Plano de Estudos: %s
                - Matéria: %s
                - Tópico Temático: %s
                %s
                
                DIRETRIZES DE ENGENHARIA DE FLASHCARDS (REGRA DE OURO):
                1. ATOMICIDADE (Princípio da Informação Mínima): Cada cartão deve testar exatamente UM conceito, distinção, prazo, regra ou exceção.
                2. FRENTE (Pergunta / Gatilho de Evocação Ativa):
                   - Faça perguntas diretas, cenários curtos ou conceitos para completar (ex: "Qual a diferença entre ato nulo e anulável quanto à convalidação?", "Qual é o quórum para aprovação de PEC?").
                   - Evite perguntas genéricas demais como "Fale sobre X".
                3. VERSO (Resposta Concisa e Precisa):
                   - Resposta clara, direta e objetiva, destacando a palavra-chave, artigo de lei, regra mnemônica ou jurisprudência dominante aplicável.
                   - Máximo de 2 a 3 frases no verso, facilitando o recall rápido durante as revisões SM-2.
                4. RELEVÂNCIA E INCIDÊNCIA:
                   - Foque nos pontos que mais caem em provas, pegadinhas frequentes de bancas e conceitos fundamentais.
                5. SEM DUPLICIDADE:
                   - Não gere cartões redundantes ou com o mesmo teor conceitual.
                
                Retorne EXCLUSIVAMENTE um objeto JSON RFC8259 puro compatível com o schema:
                {
                  "flashcards": [
                    {
                      "frente": "Pergunta ou conceito gatilho",
                      "verso": "Resposta direta e fundamentada"
                    }
                  ]
                }
                """, quantidade, contextoPlano, contextoMateria, contextoTopico, contextoSubtopico);
    }
}
