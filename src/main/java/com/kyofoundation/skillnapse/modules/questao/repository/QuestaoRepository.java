package com.kyofoundation.skillnapse.modules.questao.repository;

import com.kyofoundation.skillnapse.modules.questao.entity.Questao;
import com.kyofoundation.skillnapse.modules.questao.enums.DificuldadeQuestao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestaoRepository extends JpaRepository<Questao, UUID> {

    boolean existsByHashEnunciado(String hashEnunciado);

    Optional<Questao> findByHashEnunciado(String hashEnunciado);

    @Query("SELECT q FROM Questao q LEFT JOIN FETCH q.alternativas WHERE q.id = :id")
    Optional<Questao> findByIdComAlternativas(@Param("id") UUID id);

    @Query("""
        SELECT q FROM Questao q
        WHERE (:assuntoGeral IS NULL OR LOWER(q.assuntoGeral) LIKE LOWER(CONCAT('%', :assuntoGeral, '%')))
          AND (:topicoReferencia IS NULL OR LOWER(q.topicoReferencia) LIKE LOWER(CONCAT('%', :topicoReferencia, '%')))
          AND (:banca IS NULL OR LOWER(q.banca) LIKE LOWER(CONCAT('%', :banca, '%')))
          AND (:ano IS NULL OR q.ano = :ano)
          AND (:dificuldade IS NULL OR q.dificuldade = :dificuldade)
          AND (:termoBusca IS NULL OR LOWER(q.enunciado) LIKE LOWER(CONCAT('%', :termoBusca, '%')))
    """)
    Page<Questao> buscarComFiltros(
            @Param("assuntoGeral") String assuntoGeral,
            @Param("topicoReferencia") String topicoReferencia,
            @Param("banca") String banca,
            @Param("ano") Integer ano,
            @Param("dificuldade") DificuldadeQuestao dificuldade,
            @Param("termoBusca") String termoBusca,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT q FROM Questao q
        LEFT JOIN FETCH q.alternativas
        WHERE (:topicoTitulo <> '' AND (LOWER(q.topicoReferencia) LIKE LOWER(CONCAT('%', :topicoTitulo, '%'))
           OR LOWER(q.assuntoGeral) LIKE LOWER(CONCAT('%', :topicoTitulo, '%'))))
           OR q.id IN (
               SELECT t.questao.id FROM TentativaQuestao t WHERE t.topico.id = :topicoId
           )
        ORDER BY q.criadoEm DESC
    """)
    List<Questao> buscarQuestoesPorTopico(@Param("topicoId") UUID topicoId, @Param("topicoTitulo") String topicoTitulo);
}
