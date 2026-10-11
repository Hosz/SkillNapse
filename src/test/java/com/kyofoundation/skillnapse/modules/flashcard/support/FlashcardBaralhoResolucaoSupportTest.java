package com.kyofoundation.skillnapse.modules.flashcard.support;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Baralho;
import com.kyofoundation.skillnapse.modules.flashcard.finder.BaralhoFinder;
import com.kyofoundation.skillnapse.modules.flashcard.repository.BaralhoRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashcardBaralhoResolucaoSupportTest {

    @Mock
    private BaralhoFinder baralhoFinder;

    @Mock
    private BaralhoRepository baralhoRepository;

    @InjectMocks
    private FlashcardBaralhoResolucaoSupport support;

    private Usuario usuario;
    private PlanoEstudo plano;
    private Materia materia;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(UUID.randomUUID()).nome("Aluno").build();
        plano = PlanoEstudo.builder().id(UUID.randomUUID()).titulo("Plano Geral").build();
        materia = Materia.builder().id(UUID.randomUUID()).nome("Direito Administrativo").build();
    }

    @Test
    @DisplayName("Deve retornar baralho pelo ID quando baralhoId for fornecido explicitamente")
    void deveRetornarBaralhoPorIdQuandoFornecido() {
        UUID baralhoId = UUID.randomUUID();
        Baralho baralho = Baralho.builder().id(baralhoId).titulo("Baralho Específico").build();

        when(baralhoFinder.findById(baralhoId)).thenReturn(baralho);

        Baralho resultado = support.resolverBaralhoDestino(usuario, materia, plano, baralhoId);

        assertThat(resultado).isEqualTo(baralho);
        verify(baralhoFinder).findById(baralhoId);
        verify(baralhoRepository, never()).findByUsuarioId(any());
    }

    @Test
    @DisplayName("Deve reutilizar baralho existente da matéria")
    void deveReutilizarBaralhoExistenteDaMateria() {
        Baralho baralhoMateria = Baralho.builder()
                .id(UUID.randomUUID())
                .materia(materia)
                .titulo("Flashcards - Direito Administrativo")
                .build();

        when(baralhoRepository.findByUsuarioId(usuario.getId())).thenReturn(List.of(baralhoMateria));

        Baralho resultado = support.resolverBaralhoDestino(usuario, materia, plano, null);

        assertThat(resultado).isEqualTo(baralhoMateria);
        verify(baralhoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar novo baralho para a matéria quando usuário não possuir um")
    void deveCriarNovoBaralhoParaMateria() {
        when(baralhoRepository.findByUsuarioId(usuario.getId())).thenReturn(Collections.emptyList());
        when(baralhoRepository.save(any(Baralho.class))).thenAnswer(i -> i.getArgument(0));

        Baralho resultado = support.resolverBaralhoDestino(usuario, materia, plano, null);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTitulo()).isEqualTo("Flashcards - Direito Administrativo");
        assertThat(resultado.getMateria()).isEqualTo(materia);
        verify(baralhoRepository).save(any(Baralho.class));
    }

    @Test
    @DisplayName("Deve reutilizar primeiro baralho do usuário quando matéria for nula e houver baralhos")
    void deveReutilizarPrimeiroBaralhoQuandoMateriaNula() {
        Baralho baralhoExistente = Baralho.builder().id(UUID.randomUUID()).titulo("Meu Primeiro Baralho").build();
        when(baralhoRepository.findByUsuarioId(usuario.getId())).thenReturn(List.of(baralhoExistente));

        Baralho resultado = support.resolverBaralhoDestino(usuario, null, plano, null);

        assertThat(resultado).isEqualTo(baralhoExistente);
        verify(baralhoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar acervo geral quando matéria for nula e usuário não tiver baralhos")
    void deveCriarAcervoGeralQuandoUsuarioSemBaralhos() {
        when(baralhoRepository.findByUsuarioId(usuario.getId())).thenReturn(Collections.emptyList());
        when(baralhoRepository.save(any(Baralho.class))).thenAnswer(i -> i.getArgument(0));

        Baralho resultado = support.resolverBaralhoDestino(usuario, null, plano, null);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTitulo()).isEqualTo("Acervo Geral - Plano Geral");
        assertThat(resultado.getMateria()).isNull();
        verify(baralhoRepository).save(any(Baralho.class));
    }
}
