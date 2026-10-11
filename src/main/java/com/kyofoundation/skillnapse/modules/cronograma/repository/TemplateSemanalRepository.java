package com.kyofoundation.skillnapse.modules.cronograma.repository;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateSemanalRepository extends JpaRepository<TemplateSemanal, UUID> {

    Page<TemplateSemanal> findAllByUsuario(Usuario usuario, Pageable pageable);

    Page<TemplateSemanal> findAllByUsuarioAndAtivo(Usuario usuario, boolean ativo, Pageable pageable);

    List<TemplateSemanal> findAllByUsuarioAndAtivoTrue(Usuario usuario);

    List<TemplateSemanal> findAllByUsuarioAndAtivoTrueOrderByCriadoEmDesc(Usuario usuario);

    Optional<TemplateSemanal> findFirstByUsuarioAndAtivoTrueOrderByCriadoEmDesc(Usuario usuario);

    default Optional<TemplateSemanal> findByUsuarioAndAtivoTrue(Usuario usuario) {
        return findFirstByUsuarioAndAtivoTrueOrderByCriadoEmDesc(usuario);
    }
}
