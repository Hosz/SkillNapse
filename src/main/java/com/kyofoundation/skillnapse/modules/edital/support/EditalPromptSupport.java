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
               - Isole cirurgicamente a secção documental que corresponde de forma única e exclusiva ao identificador/perfil/categoria alvo especificado pelo usuário.
               - Descarte terminantemente qualquer nó, tópico, anexo ou menção pertencente a outros perfis, cargos ou categorias presentes no mesmo documento, impedindo contaminação cruzada.
               - Não adicione conteúdos externos, generalizações conceituais ou tópicos não expressamente delimitados no excerto do perfil selecionado.
            
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

            7. FORMATO DE SAÍDA ESTRITO:
               - Retorne OBRIGATORIAMENTE um objeto JSON RFC8259 puro e válido aderente ao schema solicitado.
               - NÃO inclua preâmbulos, títulos, cumprimentos, textos introdutórios ou comentários antes ou depois do JSON.
               - Inicie diretamente com '{' e termine com '}'.
            """;

    public PromptRequest criarPromptParaExtracao(String pdfExtraido) {
        return criarPromptParaExtracao(pdfExtraido, null);
    }

    public PromptRequest criarPromptParaExtracao(String pdfExtraido, String cargoAlvo) {
        String cargoInstrucao = (cargoAlvo != null && !cargoAlvo.isBlank())
                ? String.format("""
                ISOLAMENTO ESTRITO DE ESCOPO (BOUNDARY ENFORCEMENT):
                - Perfil/Cargo Alvo Selecionado: "%s"
                - Isole cirurgicamente a secção documental que corresponde de forma única e exclusiva a este cargo.
                - Descarte terminantemente qualquer nó, tópico, anexo ou menção pertencente a outros perfis, cargos ou categorias presentes no mesmo documento, impedindo contaminação cruzada.
                - Não adicione conteúdos externos, generalizações conceituais ou matérias não delimitadas para este cargo.

                """, cargoAlvo.trim())
                : "";

        String userPrompt = String.format("""
                %sConstrua a árvore hierárquica detalhada do conteúdo programático a partir do edital fornecido:

                --- INÍCIO DO TEXTO DO EDITAL ---
                %s
                --- FIM DO TEXTO DO EDITAL ---

                INSTRUÇÕES CRUCIAIS:
                - Extraia o nome exato do concurso e o cargo.
                - ORDENAÇÃO POR GRAFO DE DEPENDÊNCIAS CONCEITUAIS: Reordene os tópicos e subtópicos em sequência pedagógica ascendente (Fundacional/Estrutural -> Dependência Operacional -> Complexidade Progressiva), ignorando a numeração linear do texto-fonte.
                - AGRUPAMENTO POR AFINIDADE ONTOLÓGICA: Agrupe conceitos em tópicos coerentes com sua identidade de domínio (sem misturar blocos mecânicos com contextuais).
                - PRESERVAÇÃO DE FIDELIDADE (ZERO INTERPOLAÇÃO): Mantenha a integralidade e o sentido dos termos do texto original, alterando unicamente a taxonomia e a indexação sequencial de aprendizagem.
                - NUNCA resuma ou una disciplinas diferentes em uma só.
                - Garanta profundidade, riqueza e granularidade pedagógica para todas as disciplinas identificadas.
                - Retorne EXCLUSIVAMENTE o JSON estruturado puro, sem nenhum texto antes ou depois.
                """, cargoInstrucao, pdfExtraido);

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
