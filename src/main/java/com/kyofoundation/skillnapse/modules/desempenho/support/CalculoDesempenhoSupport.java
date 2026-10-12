package com.kyofoundation.skillnapse.modules.desempenho.support;

import com.kyofoundation.skillnapse.modules.desempenho.dto.response.TopicoLacunaResponse;
import com.kyofoundation.skillnapse.modules.desempenho.enums.NivelCriticidadeTopico;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CalculoDesempenhoSupport {

    private static final double LIMIAR_CRITICO = 60.0;
    private static final double LIMIAR_ESTAVEL = 75.0;
    private static final int PESO_ALTO_MINIMO = 6;
    private static final long MINIMO_TENTATIVAS_ESTAVEL = 3L;

    public double calcularTaxaAcerto(Long totalTentativas, Long totalAcertos) {
        if (totalTentativas == null || totalTentativas <= 0L || totalAcertos == null || totalAcertos <= 0L) {
            return 0.0;
        }
        double taxa = (totalAcertos.doubleValue() * 100.0) / totalTentativas.doubleValue();
        return arredondar(taxa);
    }

    public NivelCriticidadeTopico determinarCriticidade(
            Long totalTentativas,
            Double taxaAcerto,
            Integer pesoEdital,
            NivelProficiencia nivelProficiencia
    ) {
        if (totalTentativas == null || totalTentativas == 0L) {
            return NivelCriticidadeTopico.SEM_DADOS;
        }

        double taxaEfetiva = taxaAcerto != null ? taxaAcerto : 0.0;
        int pesoEfetivo = pesoEdital != null ? pesoEdital : 1;

        if (taxaEfetiva < LIMIAR_CRITICO) {
            if (pesoEfetivo >= PESO_ALTO_MINIMO || nivelProficiencia == NivelProficiencia.INICIANTE) {
                return NivelCriticidadeTopico.CRITICO;
            }
            return NivelCriticidadeTopico.ATENCAO;
        }

        if (taxaEfetiva < LIMIAR_ESTAVEL) {
            return NivelCriticidadeTopico.ATENCAO;
        }

        if (totalTentativas < MINIMO_TENTATIVAS_ESTAVEL) {
            return NivelCriticidadeTopico.ATENCAO;
        }

        return NivelCriticidadeTopico.ESTAVEL;
    }

    public double calcularIndiceSeveridade(
            Double taxaAcerto,
            Integer pesoEdital,
            Long totalTentativas
    ) {
        int peso = (pesoEdital != null && pesoEdital > 0) ? pesoEdital : 1;

        if (totalTentativas == null || totalTentativas == 0L) {
            return 0.0;
        }

        double taxa = taxaAcerto != null ? taxaAcerto : 0.0;
        double severidade = peso * (100.0 - taxa);
        return arredondar(severidade);
    }

    public double calcularReadinessIndex(List<TopicoLacunaResponse> topicos) {
        if (topicos == null || topicos.isEmpty()) {
            return 0.0;
        }

        double somaPonderada = 0.0;
        int somaPesos = 0;

        for (TopicoLacunaResponse topico : topicos) {
            int peso = (topico.pesoEdital() != null && topico.pesoEdital() > 0) ? topico.pesoEdital() : 1;
            double taxa = (topico.taxaAcerto() != null) ? topico.taxaAcerto() : 0.0;

            somaPonderada += (taxa * peso);
            somaPesos += peso;
        }

        if (somaPesos == 0) {
            return 0.0;
        }

        return arredondar(somaPonderada / somaPesos);
    }

    public double arredondar(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return 0.0;
        }
        return BigDecimal.valueOf(valor)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
