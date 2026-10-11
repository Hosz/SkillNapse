package com.kyofoundation.skillnapse.modules.cronograma.finder;

import com.kyofoundation.skillnapse.common.exception.ResourceNotFoundException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.cronograma.entity.TemplateSemanal;
import com.kyofoundation.skillnapse.modules.cronograma.repository.TemplateSemanalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateSemanalFinderTest {

    @Mock
    private TemplateSemanalRepository repository;

    @InjectMocks
    private TemplateSemanalFinder finder;

    @Test
    @DisplayName("Deve encontrar template semanal por ID com sucesso")
    void deveEncontrarPorIdComSucesso() {
        UUID id = UUID.randomUUID();
        TemplateSemanal template = TemplateSemanal.builder().id(id).nome("Padrão").build();

        when(repository.findById(id)).thenReturn(Optional.of(template));

        TemplateSemanal resultado = finder.findById(id);

        assertThat(resultado).isEqualTo(template);
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando template nao for encontrado")
    void deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> finder.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Template semanal não encontrado ou não existe.");
    }

    @Test
    @DisplayName("Deve encontrar template ativo por usuario com sucesso")
    void deveEncontrarAtivoPorUsuario() {
        Usuario usuario = Usuario.builder().id(UUID.randomUUID()).build();
        TemplateSemanal template = TemplateSemanal.builder().id(UUID.randomUUID()).ativo(true).build();

        when(repository.findByUsuarioAndAtivoTrue(usuario)).thenReturn(Optional.of(template));

        TemplateSemanal resultado = finder.findAtivoByUsuario(usuario);

        assertThat(resultado).isEqualTo(template);
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando nao houver template ativo")
    void deveLancarExcecaoQuandoNaoHouverAtivo() {
        Usuario usuario = Usuario.builder().id(UUID.randomUUID()).build();

        when(repository.findByUsuarioAndAtivoTrue(usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> finder.findAtivoByUsuario(usuario))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Nenhum template semanal ativo encontrado para o usuário.");
    }

    @Test
    @DisplayName("Deve auto-curar e retornar o mais recente quando houver multiplos templates ativos")
    void deveCurarMultiplosTemplatesAtivosERetornarMaisRecente() {
        Usuario usuario = Usuario.builder().id(UUID.randomUUID()).build();
        TemplateSemanal maisRecente = TemplateSemanal.builder().id(UUID.randomUUID()).nome("Recente").ativo(true).build();
        TemplateSemanal antigo = TemplateSemanal.builder().id(UUID.randomUUID()).nome("Antigo").ativo(true).build();

        when(repository.findAllByUsuarioAndAtivoTrueOrderByCriadoEmDesc(usuario))
                .thenReturn(new java.util.ArrayList<>(java.util.List.of(maisRecente, antigo)));

        TemplateSemanal resultado = finder.findAtivoByUsuario(usuario);

        assertThat(resultado).isEqualTo(maisRecente);
        assertThat(antigo.getAtivo()).isFalse();
        org.mockito.Mockito.verify(repository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
