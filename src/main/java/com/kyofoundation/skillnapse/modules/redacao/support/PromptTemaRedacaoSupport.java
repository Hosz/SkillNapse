package com.kyofoundation.skillnapse.modules.redacao.support;

import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import org.springframework.stereotype.Component;

@Component
public class PromptTemaRedacaoSupport {

    public String construirPromptGeracao(
            PlanoEstudo plano,
            Materia materia,
            Topico topico,
            String bancaAlvo,
            String generoTextual
    ) {
        return construirPromptGeracao(plano, materia, topico, bancaAlvo, generoTextual, null);
    }

    public String construirPromptGeracao(
            PlanoEstudo plano,
            Materia materia,
            Topico topico,
            String bancaAlvo,
            String generoTextual,
            String eixoTematico
    ) {
        String banca = (bancaAlvo != null && !bancaAlvo.isBlank())
                ? bancaAlvo.trim()
                : "Bancas Tradicionais de Concurso Público (Cebraspe/FGV/FCC)";

        String genero = (generoTextual != null && !generoTextual.isBlank())
                ? generoTextual.trim()
                : "Dissertativo-Argumentativo";

        String planoNome = (plano != null && plano.getTitulo() != null)
                ? plano.getTitulo().trim()
                : "Concurso Público e Exames de Seleção";

        String materiaNome = (materia != null && materia.getNome() != null)
                ? materia.getNome().trim()
                : (eixoTematico != null && !eixoTematico.isBlank() ? eixoTematico.trim() : "Geral / Conhecimentos Interdisciplinares");

        String topicoNome = (topico != null && topico.getTitulo() != null)
                ? topico.getTitulo().trim()
                : "Temas Contemporâneos e Relevantes ao Cargo";

        return String.format("""
                Você é um membro sênior de banca examinadora responsável pela elaboração de provas discursivas e redações de alto nível para os maiores concursos públicos do Brasil (Cebraspe/Cespe, FGV, FCC, Vunesp).

                Sua missão é formular uma PROPOSTA INÉDITA, COMPLETA E PROFISSIONAL DE REDAÇÃO orientada aos seguintes parâmetros:
                - Plano de Estudo / Concurso Alvo: %s
                - Disciplina / Eixo Temático: %s
                - Tópico Específico em Foco: %s
                - Gênero Textual Exigido: %s
                - Estilo da Banca Avaliadora: %s

                DIRETRIZES ESTRITAS DE CADA CAMPO:
                1. "titulo":
                   - DEVE SER EXCLUSIVAMENTE A FRASE-TEMA (O TEMA DA REDAÇÃO), concisa, instigante e bem delimitada (entre 5 e 16 palavras, máximo 130 caracteres).
                   - Exemplo de título válido: "Inteligência Artificial e o Mundo do Trabalho: Dilemas Éticos, Regulação e Empregabilidade".
                   - PROIBIÇÃO ABSOLUTA: NUNCA insira comandos operacionais de prova no título (PROIBIDO escrever frases como "O texto dissertativo deve ser redigido em prosa", "extensão de 20 a 30 linhas", "a avaliação pautar-se-á", etc.). O título deve ser apenas o nome do tema!

                2. "descricao":
                   - Breve introdução e delimitação reflexiva da problemática em 1 ou 2 períodos instigantes (entre 80 e 250 caracteres), situando o conflito ou dilema central a ser discutido pelo candidato.

                3. "textosMotivadores":
                   - Devem ser RIGOROSAMENTE 3 TEXTOS DE APOIO DENSOS, ENCORPADOS E PROFUNDOS (cada texto com 120 a 250 palavras, parágrafos desenvolvidos e substanciais, reproduzindo com fidelidade uma prova oficial de alto nível):
                     * TEXTO I (Marco Legal ou Teórico): Dispositivos da CF/88, leis, tratados internacionais, súmulas/jurisprudência ou conceitos doutrinários/filosóficos de autores renomados.
                     * TEXTO II (Panorama Estatístico e Pesquisa Concreta): Dados quantitativos realistas de órgãos oficiais e institutos de pesquisa (IBGE, IPEA, OCDE, ONU, Fórum Econômico Mundial, FGV), com números, taxas, porcentagens e impactos socioeconômicos.
                     * TEXTO III (Reflexão Contemporânea e Dilema Social): Análise crítica qualificada sobre casos práticos, impactos éticos nos cidadãos e o papel do Estado e da sociedade civil.
                   - Estruture os 3 textos separando-os com os marcadores formais exatos:
                     ### TEXTO I: [Subtítulo do Texto I]
                     [Texto I denso com 2 parágrafos desenvolvidos]
                     Fonte: [Indicação formal da fonte]

                     ### TEXTO II: [Subtítulo do Texto II]
                     [Texto II denso com 2 parágrafos desenvolvidos e estatísticas]
                     Fonte: [Indicação formal da fonte]

                     ### TEXTO III: [Subtítulo do Texto III]
                     [Texto III denso com 2 parágrafos desenvolvidos e análise ética]
                     Fonte: [Indicação formal da fonte]

                4. "criteriosAvaliacao":
                   - Orientações formais (extensão recomendada de 20 a 30 linhas, norma-padrão da língua portuguesa) e listar 3 tópicos norteadores pontuados de abordagem obrigatória (Padrão de Resposta / Espelho de Correção da banca).

                Retorne o resultado EXCLUSIVAMENTE em formato JSON com o seguinte schema:
                {
                  "titulo": "string",
                  "descricao": "string",
                  "textosMotivadores": "string",
                  "criteriosAvaliacao": "string"
                }
                """,
                planoNome,
                materiaNome,
                topicoNome,
                genero,
                banca
        );
    }
}
