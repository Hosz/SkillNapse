package com.kyofoundation.skillnapse.modules.cronograma.finder;

import com.kyofoundation.skillnapse.common.exception.ResourceNotFoundException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import com.kyofoundation.skillnapse.modules.cronograma.repository.TemplateSemanalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateSemanalFinder {

    private final TemplateSemanalRepository templateSemanalRepository;

    public Page<TemplateSemanal> findAllByUsuario(Usuario usuario, Pageable pageable) {
        return templateSemanalRepository.findAllByUsuario(usuario, pageable);
    }

    public Page<TemplateSemanal> findAllByUsuarioAndAtivo(Usuario usuario, boolean ativo, Pageable pageable) {
        return templateSemanalRepository.findAllByUsuarioAndAtivo(usuario, ativo, pageable);
    }

    public TemplateSemanal findById(UUID templateId) {
        return templateSemanalRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template semanal não encontrado ou não existe."));
    }

    @Transactional
    public TemplateSemanal findAtivoByUsuario(Usuario usuario) {
        List<TemplateSemanal> ativos = templateSemanalRepository.findAllByUsuarioAndAtivoTrueOrderByCriadoEmDesc(usuario);
        if (ativos.isEmpty()) {
            return templateSemanalRepository.findByUsuarioAndAtivoTrue(usuario)
                    .orElseThrow(() -> new ResourceNotFoundException("Nenhum template semanal ativo encontrado para o usuário."));
        }
        if (ativos.size() > 1) {
            for (int i = 1; i < ativos.size(); i++) {
                ativos.get(i).setAtivo(false);
            }
            templateSemanalRepository.saveAll(ativos.subList(1, ativos.size()));
        }
        return ativos.get(0);
    }

    @Transactional
    public Optional<TemplateSemanal> findAtivoOpcionalByUsuario(Usuario usuario) {
        List<TemplateSemanal> ativos = templateSemanalRepository.findAllByUsuarioAndAtivoTrueOrderByCriadoEmDesc(usuario);
        if (ativos.isEmpty()) {
            return templateSemanalRepository.findByUsuarioAndAtivoTrue(usuario);
        }
        if (ativos.size() > 1) {
            for (int i = 1; i < ativos.size(); i++) {
                ativos.get(i).setAtivo(false);
            }
            templateSemanalRepository.saveAll(ativos.subList(1, ativos.size()));
        }
        return Optional.of(ativos.get(0));
    }
}
