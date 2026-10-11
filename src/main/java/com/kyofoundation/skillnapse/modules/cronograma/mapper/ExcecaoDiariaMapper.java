package com.kyofoundation.skillnapse.modules.cronograma.mapper;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.EditarExcecaoDiariaRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.RegistrarExcecaoDiariaRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.ExcecaoDiariaResponse;
import com.kyofoundation.skillnapse.modules.cronograma.entity.BlocoHorarioTemplate;
import com.kyofoundation.skillnapse.modules.cronograma.entity.ExcecaoDiaria;
import com.kyofoundation.skillnapse.modules.cronograma.enums.DiaSemana;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class ExcecaoDiariaMapper {

    public static ExcecaoDiaria toCriarExcecao(
            Usuario usuario,
            BlocoHorarioTemplate blocoHorarioTemplate,
            LocalDate data,
            RegistrarExcecaoDiariaRequest request,
            Materia materia,
            Topico topico) {

        TipoBloco tipoBloco = request.tipoBloco() != null
                ? request.tipoBloco()
                : (blocoHorarioTemplate != null ? blocoHorarioTemplate.getTipoBloco() : TipoBloco.FOCO_TEORIA);

        return ExcecaoDiaria.builder()
                .usuario(usuario)
                .dataExcecao(data)
                .blocoTemplateOrigem(blocoHorarioTemplate)
                .tipoAcao(request.tipoAcao())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .tipoBloco(tipoBloco)
                .materia(materia)
                .topico(topico)
                .titulo(request.titulo())
                .build();
    }

    public static ExcecaoDiariaResponse toResponse(ExcecaoDiaria excecaoDiaria) {
        UUID blocoId = excecaoDiaria.getBlocoTemplateOrigem() != null ? excecaoDiaria.getBlocoTemplateOrigem().getId() : null;
        DiaSemana diaSemana = excecaoDiaria.getBlocoTemplateOrigem() != null ? excecaoDiaria.getBlocoTemplateOrigem().getDiaSemana() : null;
        UUID materiaId = excecaoDiaria.getMateria() != null ? excecaoDiaria.getMateria().getId() : null;
        String materiaNome = excecaoDiaria.getMateria() != null ? excecaoDiaria.getMateria().getNome() : null;
        UUID topicoId = excecaoDiaria.getTopico() != null ? excecaoDiaria.getTopico().getId() : null;
        String topicoTitulo = excecaoDiaria.getTopico() != null ? excecaoDiaria.getTopico().getTitulo() : null;

        return new ExcecaoDiariaResponse(
                excecaoDiaria.getId(),
                excecaoDiaria.getDataExcecao(),
                excecaoDiaria.getTipoAcao(),
                blocoId,
                diaSemana,
                excecaoDiaria.getHoraInicio(),
                excecaoDiaria.getHoraFim(),
                excecaoDiaria.getTipoBloco(),
                materiaId,
                materiaNome,
                topicoId,
                topicoTitulo,
                excecaoDiaria.getTitulo()
        );
    }

    public static void toEditarExcecao(
            EditarExcecaoDiariaRequest request,
            ExcecaoDiaria excecaoDiaria,
            BlocoHorarioTemplate blocoHorarioTemplateNovo,
            Materia materiaNova,
            Topico topicoNovo) {
        if (request == null) {
            return;
        }
        if (request.tipoAcao() != null) {
            excecaoDiaria.setTipoAcao(request.tipoAcao());
        }
        if (blocoHorarioTemplateNovo != null) {
            excecaoDiaria.setBlocoTemplateOrigem(blocoHorarioTemplateNovo);
        }
        if (request.horaInicio() != null) {
            excecaoDiaria.setHoraInicio(request.horaInicio());
        }
        if (request.horaFim() != null) {
            excecaoDiaria.setHoraFim(request.horaFim());
        }
        if (request.tipoBloco() != null) {
            excecaoDiaria.setTipoBloco(request.tipoBloco());
        }
        if (materiaNova != null) {
            excecaoDiaria.setMateria(materiaNova);
        }
        if (topicoNovo != null) {
            excecaoDiaria.setTopico(topicoNovo);
        }
        if (request.titulo() != null) {
            excecaoDiaria.setTitulo(request.titulo());
        }
    }
}
