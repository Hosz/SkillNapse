package com.kyofoundation.skillnapse.modules.edital.support;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import org.springframework.stereotype.Component;

@Component
public class EditalPromptSupport {

    private static final String SYSTEM_PROMPT = """
            Você é um especialista sênior em engenharia pedagógica e curadoria de conteúdo programático para concursos públicos e vestibulares da plataforma SkillNapse.
            Sua missão é transformar o texto bruto do edital em uma árvore hierárquica rica, estruturada e de alto valor pedagógico para o planejamento de estudos do candidato.
            
            DIRETRIZES CRÍTICAS DE ESTRUTURAÇÃO:
            
            1. NOMENCLATURA DAS MATÉRIAS:
               - Identifique as disciplinas macro exigidas (ex: "Língua Portuguesa", "Raciocínio Lógico e Matemático", "Direito Constitucional", "Direito Administrativo", "Informática e Tecnologia da Informação", "Conhecimentos Específicos").
            
            2. DECOMPOSIÇÃO OBRIGATÓRIA DOS TÓPICOS (NUNCA GENERALIZE):
               - REGRA INEGOCIÁVEL: NUNCA crie um tópico único com o mesmo nome da matéria (por exemplo, sob a matéria "Língua Portuguesa", é PROIBIDO criar um tópico chamado "Língua Portuguesa").
               - Toda matéria DEVE ser decomposta em múltiplos tópicos temáticos específicos (geralmente entre 3 e 8 tópicos por matéria).
               - Se o edital apresentar itens numerados ou tópicos separados por pontos/vírgulas (ex: "1. Morfologia e sintaxe", "2. Atos e poderes administrativos"), agrupe ou mapeie cada um desses temas como tópicos temáticos claros.
            
            3. EXTRAÇÃO MINUCIOSA DE SUBTÓPICOS:
               - Cada tópico DEVE conter uma lista de 'subtopicos' detalhando os conceitos, teorias, leis específicas, desdobramentos e itens pontuais a serem estudados.
               - NUNCA deixe a lista de subtópicos vazia se o edital detalha os assuntos daquele tópico.
               - Subtópicos devem ser frases/termos objetivos e claros (ex: "Compreensão e interpretação de textos", "Concordância verbal e nominal", "Princípios fundamentais da administração pública", "Regime jurídico dos servidores públicos").
            
            4. PESO NO EDITAL ('pesoEdital'):
               - Atribua um número inteiro de 1 a 10 para cada tópico representando sua relevância para a aprovação:
                 * 8 a 10: Tópicos de altíssima relevância, disciplinas eliminatórias ou com grande número de questões (ex: Redação, Língua Portuguesa, matérias específicas essenciais do cargo).
                 * 5 a 7: Tópicos de média a alta incidência e relevância intermediária.
                 * 1 a 4: Tópicos introdutórios, teóricos básicos ou de menor peso relativo.
            
            5. FILTRO E LIMPEZA:
               - Ignore regras burocráticas irrelevantes (isenção de taxa, locais de inscrição, recursos, prazos de pagamento). Foque estritamente no Conteúdo Programático exigido nas provas.

            6. FORMATO DE SAÍDA ESTRITO:
               - Retorne OBRIGATORIAMENTE um objeto JSON RFC8259 puro e válido aderente ao schema solicitado.
               - NÃO inclua preâmbulos, títulos, cumprimentos, textos introdutórios ou comentários antes ou depois do JSON.
               - Inicie diretamente com '{' e termine com '}'.
            """;

    public PromptRequest criarPromptParaExtracao(String pdfExtraido) {
        return criarPromptParaExtracao(pdfExtraido, null);
    }

    public PromptRequest criarPromptParaExtracao(String pdfExtraido, String cargoAlvo) {
        String cargoInstrucao = (cargoAlvo != null && !cargoAlvo.isBlank())
                ? String.format("ATENÇÃO: O foco específico do candidato é o cargo: \"%s\". Extraia EXCLUSIVAMENTE o conteúdo programático e disciplinas pertinentes a este cargo, desconsiderando matérias exclusivas de outros cargos presentes no edital.\n\n", cargoAlvo.trim())
                : "";

        String userPrompt = String.format("""
                %sConstrua a árvore hierárquica detalhada do conteúdo programático a partir do edital fornecido:

                --- INÍCIO DO TEXTO DO EDITAL ---
                %s
                --- FIM DO TEXTO DO EDITAL ---

                INSTRUÇÕES CRUCIAIS:
                - Extraia o nome exato do concurso e o cargo.
                - Decomponha CADA matéria em seus respectivos tópicos temáticos e liste TODOS os subtópicos correspondentes.
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
                   - Extraia o nome oficial do concurso público ou exame e o órgão/entidade promotora (ex: "Tribunal Regional Federal", "Secretaria de Estado da Fazenda", "Polícia Civil", "Concurso Público Nacional Unificado").
                2. VARREDURA EXAUSTIVA DE CARGOS E ESPECIALIDADES:
                   - Inspecione minuciosamente os quadros de vagas, tabelas de cargos/requisitos e seções de conteúdo programático do edital.
                   - Editais frequentemente agrupam múltiplos órgãos vinculados, diferentes níveis de escolaridade (ex: Técnico e Analista), carreiras distintas (ex: Agente e Escrivão; Soldado e Oficial) ou dezenas de especialidades (ex: Direito, Engenharia, Tecnologia da Informação, Área Administrativa).
                   - NUNCA limite o resultado apenas ao primeiro cargo citado na portaria de abertura ou preâmbulo. Inclua TODOS os cargos e especialidades identificados no certame.
                   - Mantenha o nome formal e canônico de cada cargo (ex: "Analista Judiciário - Área Judiciária", "Técnico Administrativo", "Auditor-Fiscal").
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
