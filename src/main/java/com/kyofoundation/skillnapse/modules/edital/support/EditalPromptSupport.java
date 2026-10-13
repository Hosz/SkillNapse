package com.kyofoundation.skillnapse.modules.edital.support;

import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EditalPromptSupport {

    private static final String SYSTEM_PROMPT = """
            Você é um especialista sênior em engenharia pedagógica, análise estatística e curadoria de conteúdo programático para concursos públicos e vestibulares da plataforma SkillNapse.
            Sua missão é transformar o texto bruto do edital em uma árvore hierárquica rica, estruturada e de alto valor pedagógico para o planejamento de estudos do candidato, aplicando a metodologia de RAIO-X DA BANCA.

            DIRETRIZES CRÍTICAS DE ESTRUTURAÇÃO:

            1. IDENTIFICAÇÃO DO CERTAME, BANCA E CARREIRA:
               - Identifique o nome oficial do concurso ('nomeConcurso') e o cargo alvo ('cargo').
               - Identifique com precisão a BANCA ORGANIZADORA ('banca') do certame (ex: "FGV", "Cebraspe", "FCC", "Vunesp", "Cesgranrio", "Idecan", "AOCP", "Instituto Consulplan").
               - Identifique a CARREIRA/ÁREA ('carreira') do cargo (ex: "Policial", "Fiscal/Controle", "Tribunais/Jurídica", "Administrativa", "Bancária", "Tecnologia da Informação", "Saúde").

            2. NOMENCLATURA DAS MATÉRIAS:
               - Identifique as disciplinas macro exigidas (ex: "Língua Portuguesa", "Raciocínio Lógico-Matemático", "Direito Constitucional", "Direito Administrativo", "Informática e Tecnologia da Informação", "Conhecimentos Específicos").

            3. DECOMPOSIÇÃO OBRIGATÓRIA DOS TÓPICOS (NUNCA GENERALIZE):
               - REGRA INEGOCIÁVEL: NUNCA crie um tópico único com o mesmo nome da matéria (por exemplo, sob a matéria "Língua Portuguesa", é PROIBIDO criar um tópico chamado "Língua Portuguesa").
               - Toda matéria DEVE ser decomposta em múltiplos tópicos temáticos específicos (geralmente entre 3 e 8 tópicos por matéria).
               - Se o edital apresentar itens numerados ou tópicos separados por pontos/vírgulas, agrupe ou mapeie cada um desses temas como tópicos temáticos claros.

            4. EXTRAÇÃO MINUCIOSA DE SUBTÓPICOS:
               - Cada tópico DEVE conter uma lista de 'subtopicos' detalhando os conceitos, teorias, leis específicas, desdobramentos e itens pontuais a serem estudados.
               - NUNCA deixe a lista de subtópicos vazia se o edital detalha os assuntos daquele tópico.
               - Subtópicos devem ser frases/termos objetivos e claros (ex: "Compreensão e interpretação de textos", "Concordância verbal e nominal", "Princípios fundamentais da administração pública", "Regime jurídico dos servidores públicos").

            5. METODOLOGIA DE RAIO-X DA BANCA E PESO CALIBRADO ('pesoEdital', 'relevanciaBanca', 'justificativaPeso'):
               - Cruze as matérias e tópicos exigidos no edital com o HISTÓRICO ESTATÍSTICO DE PROVAS REAIS da BANCA organizadora nos ÚLTIMOS 3 A 5 ANOS para essa CARREIRA/ÁREA.
               - O 'pesoEdital' DEVE ser um número inteiro de 1 a 10 refletindo o equilíbrio entre a pontuação/questões formais do edital e a RECORRÊNCIA ESTATÍSTICA REAL em provas anteriores da banca:
                 * 9 ou 10: Tópicos de altíssima incidência histórica da banca (tópicos mais cobrados nas provas recentes dessa carreira) ou disciplinas eliminatórias com maior volume de questões.
                 * 7 ou 8: Tópicos de média-alta recorrência, cobrados de forma frequente e constante pela banca.
                 * 5 ou 6: Tópicos de incidência intermediária ou moderada.
                 * 1 a 4: Tópicos introdutórios, teóricos básicos ou raramente exigidos em provas recentes da banca.
               - Preencha 'relevanciaBanca' com: "MUITO_ALTA", "ALTA", "MEDIA" ou "BAIXA".
               - Preencha 'justificativaPeso' com uma justificativa pedagógica transparente e objetiva citando a banca e a tendência histórica (ex: "A FGV cobrou interpretação de texto e tipologia em cerca de 40% das questões de Português em certames policiais recentes (2022-2025).").

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
        String textoOtimizado = extrairTrechoParaConteudoProgramatico(pdfExtraido, cargoAlvo);

        String cargoInstrucao = (cargoAlvo != null && !cargoAlvo.isBlank())
                ? String.format("ATENÇÃO: O foco específico do candidato é o cargo: \"%s\". Extraia EXCLUSIVAMENTE o conteúdo programático e disciplinas pertinentes a este cargo, desconsiderando matérias exclusivas de outros cargos presentes no edital.\n\n", cargoAlvo.trim())
                : "";

        String userPrompt = String.format("""
                %sConstrua a árvore hierárquica detalhada do conteúdo programático a partir do edital fornecido:

                --- INÍCIO DO TEXTO DO EDITAL ---
                %s
                --- FIM DO TEXTO DO EDITAL ---

                INSTRUÇÕES CRUCIAIS:
                - Identifique o nome do concurso, a BANCA organizadora, a CARREIRA/ÁREA e o cargo.
                - Decomponha CADA matéria em seus respectivos tópicos temáticos e liste TODOS os subtópicos correspondentes.
                - Calibre o 'pesoEdital', 'relevanciaBanca' e elabore a 'justificativaPeso' com base no Raio-X da banca para essa carreira nos últimos 3 a 5 anos.
                - NUNCA resuma ou una disciplinas diferentes em uma só.
                - Garanta profundidade, riqueza e granularidade pedagógica para todas as disciplinas identificadas.
                - Retorne EXCLUSIVAMENTE o JSON estruturado puro, sem nenhum texto antes ou depois.
                """, cargoInstrucao, textoOtimizado);

        return new PromptRequest(
                userPrompt,
                SYSTEM_PROMPT,
                0.2,
                16384,
                Map.of("googleSearch", true)
        );
    }

    public PromptRequest criarPromptParaDeteccaoCargos(String pdfExtraido) {
        String textoOtimizado = extrairTrechoParaDeteccaoCargos(pdfExtraido);

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
                """, textoOtimizado);

        return new PromptRequest(
                userPrompt,
                systemPrompt,
                0.1,
                8192
        );
    }

    public static String extrairTrechoParaDeteccaoCargos(String textoCompleto) {
        if (textoCompleto == null || textoCompleto.isBlank()) {
            return "";
        }

        // Se o texto já tiver tamanho compacto (até ~25.000 caracteres), envia completo
        if (textoCompleto.length() <= 25_000) {
            return textoCompleto;
        }

        StringBuilder trecho = new StringBuilder();

        // 1. Primeiros 20.000 caracteres (preâmbulo, identificação do órgão, comissão e primeiras tabelas)
        trecho.append(textoCompleto, 0, 20_000);
        trecho.append("\n\n[... TRECHO INTERMEDIÁRIO OMITIDO PARA ANÁLISE FOCADA DE CARGOS ...]\n\n");

        // 2. Busca por seções específicas de quadros de vagas/cargos no restante do texto
        String restante = textoCompleto.substring(20_000);
        Pattern padraoQuadro = Pattern.compile(
                "(?i)(quadro\\s+de\\s+vagas|dos\\s+cargos|tabela\\s+de\\s+cargos|cargos\\s+e\\s+vagas|distribui[cç][aã]o\\s+das\\s+vagas|anexo\\s+[i1]\\b)",
                Pattern.UNICODE_CHARACTER_CLASS
        );

        Matcher matcher = padraoQuadro.matcher(restante);
        int trechosExtras = 0;
        int maxTrechosExtras = 3;

        while (matcher.find() && trechosExtras < maxTrechosExtras) {
            int matchStart = matcher.start();
            int startJanela = Math.max(0, matchStart - 100);
            int endJanela = Math.min(restante.length(), matchStart + 3500);

            trecho.append("\n--- SEÇÃO DE VAGAS/CARGOS IDENTIFICADA NO EDITAL ---\n");
            trecho.append(restante, startJanela, endJanela);
            trecho.append("\n");
            trechosExtras++;
        }

        return trecho.toString();
    }

    public static String extrairTrechoParaConteudoProgramatico(String textoCompleto, String cargoAlvo) {
        if (textoCompleto == null || textoCompleto.isBlank()) {
            return "";
        }

        // Se o texto for razoável (até ~80.000 caracteres, cerca de 35 páginas), envia completo
        if (textoCompleto.length() <= 80_000) {
            return textoCompleto;
        }

        StringBuilder trecho = new StringBuilder();

        // 1. Primeiros 15.000 caracteres (identificação do concurso, banca, quadro geral de provas e pesos)
        trecho.append(textoCompleto, 0, 15_000);
        trecho.append("\n\n[... REGRAS BUROCRÁTICAS E PROCEDIMENTAIS OMITIDAS PARA PROCESSAMENTO DE CONTEÚDO ...]\n\n");

        // 2. Localiza onde se inicia a seção de Conteúdo Programático
        Pattern padraoConteudo = Pattern.compile(
                "(?i)(conte[uú]do\\s+program[aá]tico|objetos\\s+de\\s+avalia[cç][aã]o|programa\\s+das\\s+provas|programa\\s+de\\s+conhecimentos|disciplinas\\s+e\\s+conte[uú]dos|anexo\\s+.*conte[uú]do)",
                Pattern.UNICODE_CHARACTER_CLASS
        );

        Matcher matcher = padraoConteudo.matcher(textoCompleto);
        if (matcher.find()) {
            int indiceInicio = matcher.start();
            int fimJanela = Math.min(textoCompleto.length(), indiceInicio + 75_000);
            trecho.append("--- INÍCIO DO CONTEÚDO PROGRAMÁTICO IDENTIFICADO ---\n");
            trecho.append(textoCompleto, indiceInicio, fimJanela);
        } else {
            // Fallback: se não encontrar os termos, inclui os anexos finais do documento
            int inicioFinal = Math.max(15_000, textoCompleto.length() - 70_000);
            trecho.append("--- ANEXOS FINAIS DO EDITAL ---\n");
            trecho.append(textoCompleto, inicioFinal, textoCompleto.length());
        }

        return trecho.toString();
    }
}
