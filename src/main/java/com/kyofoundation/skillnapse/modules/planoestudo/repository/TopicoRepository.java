package com.kyofoundation.skillnapse.modules.planoestudo.repository;

import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TopicoRepository extends JpaRepository<Topico, UUID> {
    @Query("SELECT t FROM Topico t WHERE t.materia = :materia AND t.topicoPai IS NULL ORDER BY t.ordem ASC, t.criadoEm ASC")
    List<Topico> findByMateriaAndTopicoPaiIsNullOrderByOrdemAsc(@Param("materia") Materia materia);

    List<Topico> findByMateriaOrderByOrdemAsc(Materia materia);
    List<Topico> findByMateriaIn(List<Materia> materias);
}
