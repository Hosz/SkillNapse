package com.kyofoundation.skillnapse.modules.planoestudo.repository;

import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MateriaRepository extends JpaRepository<Materia, UUID> {
    @Query("SELECT m FROM Materia m WHERE m.planoEstudo = :planoEstudo ORDER BY m.ordem ASC, m.criadoEm ASC")
    Page<Materia> findByPlanoEstudo(@Param("planoEstudo") PlanoEstudo planoEstudo, Pageable pageable);

    List<Materia> findByPlanoEstudoOrderByOrdemAsc(PlanoEstudo planoEstudo);
    List<Materia> findByPlanoEstudoOrderByOrdemAscCriadoEmAsc(PlanoEstudo planoEstudo);
}
