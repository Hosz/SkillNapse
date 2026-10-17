package com.kyofoundation.skillnapse.modules.sessao.support;

import com.kyofoundation.skillnapse.modules.sessao.dto.response.ResumoHorasLiquidasResponse;
import com.kyofoundation.skillnapse.modules.sessao.repository.TotalizadorSessaoProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class ResumoHorasLiquidasSupport {
    public ResumoHorasLiquidasResponse calcularResumo(TotalizadorSessaoProjection dadosBrutos) {
        if (dadosBrutos == null) {
            return new ResumoHorasLiquidasResponse(0L, 0.0, 0L, 0L, 0L);
        }

        long segundos = dadosBrutos.getTotalSegundos() != null ? dadosBrutos.getTotalSegundos() : 0L;
        long concluidas = dadosBrutos.getTotalConcluidas() != null ? dadosBrutos.getTotalConcluidas() : 0L;
        long interrompidas = dadosBrutos.getTotalInterrompidas() != null ? dadosBrutos.getTotalInterrompidas() : 0L;
        long totalSessoes = concluidas + interrompidas;

        double horasLiquidas = BigDecimal.valueOf(segundos)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP)
                .doubleValue();

        return new ResumoHorasLiquidasResponse(
                segundos,
                horasLiquidas,
                concluidas,
                interrompidas,
                totalSessoes
        );
    }

    public ZoneId resolverZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }
}
