package com.kyofoundation.skillnapse.modules.edital.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class EditalCargoSupport {

    private static final Pattern PREFIXO_CARGO = Pattern.compile(
            "^(?i)(?:cargo(?:\\s+n[ºo])?\\s*\\d*\\s*[:\\-–]?\\s*|especialidade\\s*[:\\-–]?\\s*|fun[cç][aã]o\\s*[:\\-–]?\\s*|\\d+[.\\-]\\s*)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern CABECALHO_INVALIDO = Pattern.compile(
            "^(?i)(?:anexo|quadro\\s+de\\s+vagas|conte[uú]do\\s+program[aá]tico|disposi[cç][oõ]es|cronograma|resultado|tabela|itens?|requisitos?|atribui[cç][oõ]es)\\b.*",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern PADRAO_CARGO_TEXTO = Pattern.compile(
            "(?im)^\\s*(?:cargo(?:\\s+n[ºo])?\\s*\\d*|especialidade|fun[cç][aã]o)\\s*[:\\-–]\\s*([A-Za-zÀ-ÿ0-9 /\\-–()]{3,80})(?=[\\r\\n;.]|$)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    public List<String> enriquecerCargosSeNecessario(String textoPdf, List<String> cargosExistentes) {
        Map<String, String> mapaNormalizado = new LinkedHashMap<>();

        // 1. Sanitizar e normalizar cargos previamente detectados pela IA
        if (cargosExistentes != null) {
            for (String cargo : cargosExistentes) {
                adicionarSeValido(mapaNormalizado, cargo);
            }
        }

        // 2. Se a lista estiver vazia ou com apenas 1 cargo, enriquecer a partir de padrões textuais canônicos de cargos no edital
        if (textoPdf != null && mapaNormalizado.size() <= 1) {
            Matcher matcher = PADRAO_CARGO_TEXTO.matcher(textoPdf);
            while (matcher.find()) {
                String grupo = matcher.group(1);
                adicionarSeValido(mapaNormalizado, grupo);
            }
        }

        // 3. Fallback seguro caso nenhum cargo válido tenha sido identificado
        if (mapaNormalizado.isEmpty()) {
            return List.of("Geral");
        }

        return new ArrayList<>(mapaNormalizado.values());
    }

    private void adicionarSeValido(Map<String, String> mapa, String cargoBruto) {
        String limpo = sanitizarNomeCargo(cargoBruto);
        if (ehCargoValido(limpo)) {
            String chaveCaseInsensitive = limpo.toLowerCase();
            mapa.putIfAbsent(chaveCaseInsensitive, limpo);
        }
    }

    private String sanitizarNomeCargo(String cargo) {
        if (cargo == null) {
            return "";
        }
        // Primeiro remove aspas e pontuações periféricas
        String limpo = cargo.trim().replaceAll("^[\"']+|[\"';,.]+$", "").trim();
        // Remove prefixos como "Cargo 01: ", "Especialidade: "
        String semPrefixo = PREFIXO_CARGO.matcher(limpo).replaceFirst("").trim();
        // Só adota semPrefixo se não esvaziou a string (ex: "Cargo 1" não deve virar vazio)
        if (!semPrefixo.isBlank()) {
            limpo = semPrefixo;
        }
        return limpo.replaceAll("^[\"']+|[\"';,.]+$", "").trim();
    }

    private boolean ehCargoValido(String cargo) {
        if (cargo == null || cargo.isBlank() || cargo.length() < 3 || cargo.length() > 100) {
            return false;
        }
        return !CABECALHO_INVALIDO.matcher(cargo).matches();
    }
}
