package com.kyofoundation.skillnapse.modules.edital.support;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.edital.dto.request.ConverterRascunhoEditalRequest;
import com.kyofoundation.skillnapse.modules.edital.dto.response.ConversaoEditalResponse;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalArvoreEstruturada;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalMaterialItem;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalTopicoItem;
import com.kyofoundation.skillnapse.modules.edital.entity.RascunhoEdital;
import com.kyofoundation.skillnapse.modules.edital.enums.StatusRascunhoEdital;
import com.kyofoundation.skillnapse.modules.edital.mapper.EditalTreeMapper;
import com.kyofoundation.skillnapse.modules.edital.repository.RascunhoEditalRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.RelevanciaBanca;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.MateriaRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.PlanoEstudoRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EditalConversaoSupport {

    private static final List<String> PALETA_CORES = List.of(
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#06B6D4", "#84CC16"
    );

    private final PlanoEstudoRepository planoEstudoRepository;
    private final MateriaRepository materiaRepository;
    private final TopicoRepository topicoRepository;
    private final RascunhoEditalRepository rascunhoEditalRepository;

    public ConversaoEditalResponse executarConversao(Usuario usuario, RascunhoEdital rascunhoEdital, ConverterRascunhoEditalRequest request) {
        EditalArvoreEstruturada arvore = EditalTreeMapper.fromJson(rascunhoEdital.getConteudoExtraidoJson());
        if (arvore == null || arvore.materias() == null || arvore.materias().isEmpty()) {
            throw new BadRequestException("O rascunho não possui matérias válidas para conversão.");
        }

        String descricao = request.descricaoPlano() != null && !request.descricaoPlano().isBlank()
                ? request.descricaoPlano()
                : "Plano gerado automaticamente a partir do edital " + rascunhoEdital.getNomeArquivo();

        String tituloPlano = normalizarTituloPlano(request.tituloPlano(), rascunhoEdital.getNomeArquivo());

        PlanoEstudo plano = PlanoEstudo.builder()
                .usuario(usuario)
                .titulo(tituloPlano)
                .descricao(descricao)
                .banca(normalizarBanca(arvore.banca()))
                .ativo(true)
                .build();
        plano = planoEstudoRepository.save(plano);

        int totalMaterias = 0;
        int totalTopicos = 0;
        int corIndex = 0;

        for (int i = 0; i < arvore.materias().size(); i++) {
            EditalMaterialItem itemMateria = arvore.materias().get(i);
            String corHex = PALETA_CORES.get(corIndex % PALETA_CORES.size());
            corIndex++;

            Materia materia = Materia.builder()
                    .planoEstudo(plano)
                    .nome(normalizarNomeMateria(itemMateria.nome()))
                    .corHex(corHex)
                    .ordem(i)
                    .build();
            materia = materiaRepository.save(materia);
            totalMaterias++;

            if (itemMateria.topicos() != null) {
                for (int j = 0; j < itemMateria.topicos().size(); j++) {
                    EditalTopicoItem itemTopico = itemMateria.topicos().get(j);
                    int peso = (itemTopico.pesoEdital() != null && itemTopico.pesoEdital() >= 1 && itemTopico.pesoEdital() <= 10)
                            ? itemTopico.pesoEdital() : 1;

                    RelevanciaBanca relevancia = parseRelevancia(itemTopico.relevanciaBanca());
                    String justificativa = normalizarJustificativa(itemTopico.justificativaPeso());

                    Topico topicoPai = Topico.builder()
                            .materia(materia)
                            .topicoPai(null)
                            .titulo(normalizarTitulo(itemTopico.titulo()))
                            .pesoEdital(peso)
                            .relevanciaBanca(relevancia)
                            .justificativaPeso(justificativa)
                            .nivelProficiencia(NivelProficiencia.INICIANTE)
                            .concluido(false)
                            .ordem(j)
                            .build();
                    topicoPai = topicoRepository.save(topicoPai);
                    totalTopicos++;

                    if (itemTopico.subtopicos() != null) {
                        for (int k = 0; k < itemTopico.subtopicos().size(); k++) {
                            String subTitulo = itemTopico.subtopicos().get(k);
                            Topico subtopico = Topico.builder()
                                    .materia(materia)
                                    .topicoPai(topicoPai)
                                    .titulo(normalizarTitulo(subTitulo))
                                    .pesoEdital(peso)
                                    .relevanciaBanca(relevancia)
                                    .justificativaPeso(justificativa)
                                    .nivelProficiencia(NivelProficiencia.INICIANTE)
                                    .concluido(false)
                                    .ordem(k)
                                    .build();
                            topicoRepository.save(subtopico);
                            totalTopicos++;
                        }
                    }
                }
            }
        }
        rascunhoEdital.setPlanoEstudo(plano);
        rascunhoEdital.setStatus(StatusRascunhoEdital.CONVERTIDO);
        rascunhoEditalRepository.save(rascunhoEdital);

        return new ConversaoEditalResponse(
                rascunhoEdital.getId(),
                plano.getId(),
                plano.getTitulo(),
                totalMaterias,
                totalTopicos
        );
    }

    private RelevanciaBanca parseRelevancia(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        try {
            return RelevanciaBanca.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizarJustificativa(String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            return null;
        }
        String limpo = justificativa.trim();
        return limpo.length() > 1000 ? limpo.substring(0, 1000).trim() : limpo;
    }

    private String normalizarBanca(String banca) {
        if (banca == null || banca.isBlank()) {
            return null;
        }
        String limpo = banca.trim();
        return limpo.length() > 100 ? limpo.substring(0, 100).trim() : limpo;
    }

    private String normalizarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return "Tópico sem título";
        }
        String limpo = titulo.trim();
        return limpo.length() > 1000 ? limpo.substring(0, 1000).trim() : limpo;
    }

    private String normalizarNomeMateria(String nome) {
        if (nome == null || nome.isBlank()) {
            return "Matéria sem nome";
        }
        String limpo = nome.trim();
        return limpo.length() > 255 ? limpo.substring(0, 255).trim() : limpo;
    }

    private String normalizarTituloPlano(String titulo, String nomeArquivo) {
        if (titulo != null && !titulo.isBlank()) {
            String limpo = titulo.trim();
            return limpo.length() > 255 ? limpo.substring(0, 255).trim() : limpo;
        }
        String fallback = "Plano - " + (nomeArquivo != null ? nomeArquivo : "Edital");
        return fallback.length() > 255 ? fallback.substring(0, 255).trim() : fallback;
    }
}
