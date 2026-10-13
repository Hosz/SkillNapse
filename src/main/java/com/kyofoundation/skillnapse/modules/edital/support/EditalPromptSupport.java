package com.kyofoundation.skillnapse.modules.edital.support;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import org.springframework.stereotype.Component;

@Component
public class EditalPromptSupport {

    private static final String SYSTEM_PROMPT = """
            Você é um especialista sênior em engenharia pedagógica e curadoria de conteúdo programático para concursos públicos e vestibulares da plataforma SkillNapse.
            Sua missão é transformar o texto bruto do edital em uma árvore hierárquica rica, estruturada e de alto valor pedagógico para o planejamento de estudos do candidato.
            
            DIRETRIZ DE EXTRAÇÃO, ISOLAMENTO DE ESCOPO E ESTRUTURAÇÃO PEDAGÓGICA:
            
            1. ISOLAMENTO ESTRITO DE ESCOPO (BOUNDARY ENFORCEMENT):
               - Isole cirurgicamente a secção documental que corresponde ao perfil/cargo alvo especificado pelo usuário.
               - Verifique o quadro de provas e o programa de disciplinas do cargo alvo: extraia unicamente as matérias que compõem a prova desse cargo.
               - Descarte terminantemente qualquer disciplina, anexo ou tópico exclusivo de outros cargos ou outras carreiras presentes no mesmo edital (zero contaminação cruzada entre cargos).
               - Não adicione conteúdos externos ou matérias não delimitadas para o cargo selecionado.
            
            2. ORDENAÇÃO POR GRAFO DE DEPENDÊNCIAS CONCEITUAIS (PEDAGOGICAL ORDERING):
               - Ignore a sequência linear de numeração ou índice do texto-fonte quando esta não refletir dependência didática.
               - Reordene a sequência de apresentação de forma ascendente, aplicando topologia de pré-requisitos:
                 a) Fundacional/Estrutural: termos fundamentais, definições de base, regras gerais e morfologia do domínio antes de regras de concordância, exceções ou aplicações compostas.
                 b) Dependência Operacional: todo conceito que serve de premissa, base analítica ou ferramenta de resolução para outro deve preceder obrigatoriamente o tópico que dele decorre.
                 c) Complexidade Progressiva: do elementar para o complexo; do isolado para o articulado; da teoria fundacional para a aplicação analítica ou interpretativa.
               - O mesmo ordenamento por dependência conceitual aplica-se estritamente à sequência dos 'subtopicos' dentro de cada tópico.
            
            3. AGRUPAMENTO POR AFINIDADE ONTOLÓGICA (COERÊNCIA TAXONÔMICA):
               - Agrupe subtópicos em módulos de acordo com a sua identidade de domínio, garantindo que conceitos que operam em conjunto pertençam ao mesmo ramo hierárquico.
               - Evite alocar tópicos de aplicação semântica, estilística ou contextual dentro de blocos estritamente mecânicos/estruturais, e vice-versa.
               - REGRA INEGOCIÁVEL: NUNCA crie um tópico único com o mesmo nome da matéria. Toda matéria DEVE ser decomposta em múltiplos tópicos (geralmente entre 3 e 8 tópicos por matéria).
            
            4. PRESERVAÇÃO DE FIDELIDADE (ZERO INTERPOLAÇÃO):
               - Mantenha a integralidade e o sentido dos termos do texto original, alterando unicamente a taxonomia (agrupamentos em nós superiores) e a indexação sequencial de aprendizagem (ordem de execução).
               - NUNCA resuma ou una disciplinas diferentes em uma só.
            
            5. PESO NO EDITAL ('pesoEdital'):
               - Atribua um número inteiro de 1 a 10 para cada tópico representando sua relevância para a aprovação:
                 * 8 a 10: Tópicos de altíssima relevância, disciplinas eliminatórias ou com grande número de questões.
                 * 5 a 7: Tópicos de média a alta incidência e relevância intermediária.
                 * 1 a 4: Tópicos introdutórios, teóricos básicos ou de menor peso relativo.
            
            6. FILTRO E LIMPEZA:
               - Ignore regras burocráticas irrelevantes (isenção de taxa, locais de inscrição, recursos, prazos de pagamento). Foque estritamente no Conteúdo Programático exigido nas provas.

            7. FORMATO DE SAÍDA ESTRITO (SCHEMA OBRIGATÓRIO):
               - Retorne OBRIGATORIAMENTE um objeto JSON RFC8259 puro e válido aderente exatamente ao schema:
                 {
                   "nomeConcurso": "Nome Oficial do Concurso",
                   "cargo": "Nome do Cargo",
                   "materias": [
                     {
                       "nome": "Nome da Matéria",
                       "topicos": [
                         {
                           "titulo": "Título do Tópico",
                           "pesoEdital": 8,
                           "subtopicos": ["Subtópico 1", "Subtópico 2"]
                         }
                       ]
                     }
                   ]
                 }
               - É ESTRITAMENTE PROIBIDO retornar o array 'materias' vazio! Toda matéria deve conter seus tópicos, e cada tópico seus subtópicos.
               - NÃO inclua preâmbulos, títulos, cumprimentos, textos introdutórios ou comentários antes ou depois do JSON.
               - Inicie diretamente com '{' e termine com '}'.
            """;

    public PromptRequest criarPromptParaExtracao(String pdfExtraido) {
        return criarPromptParaExtracao(pdfExtraido, null);
    }

    public PromptRequest criarPromptParaExtracao(String pdfExtraido, String cargoAlvo) {
        String cargoInstrucao = (cargoAlvo != null && !cargoAlvo.isBlank())
                ? String.format("""
                ISOLAMENTO ESTRITO DE ESCOPO (BOUNDARY ENFORCEMENT) - CARGO ALVO:
                - Perfil/Cargo Alvo Selecionado: "%s"
                - Isole cirurgicamente a secção documental e o quadro de provas do cargo "%s".
                - REGRAS MANDATÓRIAS DE SELEÇÃO DE DISCIPLINAS:
                  1. Localize a estrutura de provas e o conteúdo programático do cargo "%s".
                  2. Extraia EXCLUSIVAMENTE as disciplinas que constam na prova deste cargo.
                  3. Se o edital contiver um bloco de matérias gerais comuns, inclua apenas as matérias que são cobradas na prova do cargo "%s".
                  4. PROIBIÇÃO ABSOLUTA DE CONTAMINAÇÃO CRUZADA: É terminantemente PROIBIDO incluir matérias, tópicos ou anexos que pertençam com exclusividade a OUTROS cargos do edital (ex: se o cargo for Soldado, NUNCA inclua matérias exclusivas de Oficial; se for Técnico, NUNCA inclua matérias de Analista; se for Agente, NUNCA inclua matérias de Perito ou Delegado).
                  5. Qualquer disciplina que não faça parte da prova do cargo "%s" DEVE ser descartada sumariamente.
                - Não adicione conteúdos externos ou matérias não previstas na grade deste cargo.

                """, cargoAlvo.trim(), cargoAlvo.trim(), cargoAlvo.trim(), cargoAlvo.trim(), cargoAlvo.trim())
                : "";

        String cargoAlvoLembrete = (cargoAlvo != null && !cargoAlvo.isBlank())
                ? String.format("- BLINDAGEM DE CARGO: O candidato escolheu o cargo \"%s\". Certifique-se de que TODAS as matérias retornadas são cobradas para este cargo específico. NUNCA misture disciplinas exclusivas de outros cargos.\n", cargoAlvo.trim())
                : "";

        String userPrompt = String.format("""
                %sConstrua a árvore hierárquica detalhada do conteúdo programático a partir do edital fornecido:

                --- INÍCIO DO TEXTO DO EDITAL ---
                %s
                --- FIM DO TEXTO DO EDITAL ---

                INSTRUÇÕES CRUCIAIS:
                - Extraia o nome exato do concurso e o cargo.
                %s- ORDENAÇÃO POR GRAFO DE DEPENDÊNCIAS CONCEITUAIS: Reordene os tópicos e subtópicos em sequência pedagógica ascendente (Fundacional/Estrutural -> Dependência Operacional -> Complexidade Progressiva), ignorando a numeração linear do texto-fonte.
                - AGRUPAMENTO POR AFINIDADE ONTOLÓGICA: Agrupe conceitos em tópicos coerentes com sua identidade de domínio (sem misturar blocos mecânicos com contextuais).
                - PRESERVAÇÃO DE FIDELIDADE (ZERO INTERPOLAÇÃO): Mantenha a integralidade e o sentido dos termos do texto original, alterando unicamente a taxonomia e a indexação sequencial de aprendizagem.
                - NUNCA resuma ou una disciplinas diferentes em uma só.
                - Garanta profundidade, riqueza e granularidade pedagógica para todas as disciplinas identificadas.
                - O array 'materias' NUNCA deve ser retornado vazio. Extraia todas as matérias programáticas com seus respectivos tópicos e subtópicos.
                - Retorne EXCLUSIVAMENTE o JSON estruturado puro, sem nenhum texto antes ou depois.
                """, cargoInstrucao, pdfExtraido, cargoAlvoLembrete);

        return new PromptRequest(
                userPrompt,
                SYSTEM_PROMPT,
                0.2,
                16384
        );
    }

    public PromptRequest criarPromptParaDeteccaoCargos(String pdfExtraido) {
        String systemPrompt = """
                Você é um especialista sênior em curadoria pedagógica e análise de editais de concursos públicos, vestibulares e exames de seleção brasileiros.
                Sua missão é identificar com precisão o certame e extrair a lista exaustiva de TODOS os cargos, carreiras, funções ou especialidades oferecidos no edital.

                DIRETRIZES E REGRAS MANDATÓRIAS:
                1. NOME OFICIAL DO CERTAME:
                   - Extraia o nome oficial do concurso público ou exame e o órgão ou entidade promotora.
                2. VARREDURA EXAUSTIVA DE CARGOS E ESPECIALIDADES:
                   - Inspecione minuciosamente os quadros de vagas, tabelas de cargos/requisitos e seções de conteúdo programático do edital.
                   - Editais frequentemente agrupam múltiplos órgãos vinculados, diferentes níveis de escolaridade, carreiras distintas ou dezenas de especialidades.
                   - NUNCA limite o resultado apenas ao primeiro cargo citado na portaria de abertura ou preâmbulo. Inclua TODOS os cargos e especialidades identificados no certame.
                   - Mantenha o nome formal e canônico de cada cargo conforme informado no edital.
                3. EDITAIS DE CARGO ÚNICO:
                   - Se o edital for destinado exclusivamente a um único cargo, retorne uma lista contendo apenas esse cargo canônico.
                4. NÃO adicione matérias, tópicos ou outras seções nesta etapa.
                5. Retorne EXCLUSIVAMENTE um objeto JSON RFC8259 puro com o schema:
                {
                  "nomeConcurso": "Nome Oficial do Concurso",
                  "cargos": ["Cargo 1", "Cargo 2"]
                }
                """;

        String userPrompt = String.format("""
                Identifique o nome do concurso e liste todos os cargos disponíveis no edital a seguir:

                --- INÍCIO DO TEXTO DO EDITAL ---
                %s
                --- FIM DO TEXTO DO EDITAL ---

                Retorne EXCLUSIVAMENTE o JSON estruturado puro, sem nenhum texto antes ou depois.
                """, pdfExtraido);

        return new PromptRequest(
                userPrompt,
                systemPrompt,
                0.1,
                8192
        );
    }
}
