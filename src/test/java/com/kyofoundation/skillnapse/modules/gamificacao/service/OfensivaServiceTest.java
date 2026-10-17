package com.kyofoundation.skillnapse.modules.gamificacao.service;

import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.ProgressoMetaDiariaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.dto.response.StatusOfensivaResponse;
import com.kyofoundation.skillnapse.modules.gamificacao.entity.MetaDiaria;
import com.kyofoundation.skillnapse.modules.gamificacao.entity.OfensivaUsuario;
import com.kyofoundation.skillnapse.modules.gamificacao.finder.MetaDiariaFinder;
import com.kyofoundation.skillnapse.modules.gamificacao.finder.OfensivaUsuarioFinder;
import com.kyofoundation.skillnapse.modules.gamificacao.repository.OfensivaUsuarioRepository;
import com.kyofoundation.skillnapse.modules.gamificacao.support.CalculoOfensivaSupport;
import com.kyofoundation.skillnapse.modules.gamificacao.support.ProgressoMetaDiariaSupport;
import com.kyofoundation.skillnapse.modules.questao.repository.TentativaQuestaoRepository;
import com.kyofoundation.skillnapse.modules.sessao.repository.SessaoEstudoRepository;
import com.kyofoundation.skillnapse.modules.sessao.repository.TotalizadorSessaoProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfensivaServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private OfensivaUsuarioFinder ofensivaUsuarioFinder;

    @Mock
    private OfensivaUsuarioRepository ofensivaUsuarioRepository;

    @Mock
    private CalculoOfensivaSupport calculoOfensivaSupport;

    @Mock
    private MetaDiariaFinder metaDiariaFinder;

    @Mock
    private SessaoEstudoRepository sessaoEstudoRepository;

    @Mock
    private TentativaQuestaoRepository tentativaQuestaoRepository;

    @Mock
    private ProgressoMetaDiariaSupport progressoMetaDiariaSupport;

    @InjectMocks
    private OfensivaService ofensivaService;

    private Usuario usuario;
    private OfensivaUsuario ofensiva;
    private MetaDiaria meta;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(UUID.randomUUID()).ativo(true).build();
        ofensiva = OfensivaUsuario.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .diasConsecutivosAtual(3)
                .maiorSequenciaDias(5)
                .dataUltimoEstudo(LocalDate.now().minusDays(1))
                .build();
        meta = MetaDiaria.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .metaMinutosEstudo(120)
                .metaQuestoesResolvidas(0)
                .build();
    }

    @Test
    @DisplayName("[obterStatusOfensiva] Deve consultar status e salvar se houver quebra de streak")
    void deveObterStatusOfensivaComQuebra() {
        LocalDate hoje = LocalDate.now();
        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        when(ofensivaUsuarioFinder.buscarOuCriar(usuario)).thenReturn(ofensiva);
        when(calculoOfensivaSupport.revalidarStreak(eq(ofensiva), eq(hoje))).thenReturn(true);

        when(metaDiariaFinder.buscarOuCriar(usuario)).thenReturn(meta);
        when(sessaoEstudoRepository.obterDadosAgregados(eq(usuario.getId()), isNull(), any(Instant.class), any(Instant.class)))
                .thenReturn(null);
        when(tentativaQuestaoRepository.contarQuestoesRespondidasNoIntervalo(eq(usuario.getId()), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        ProgressoMetaDiariaResponse progresso = new ProgressoMetaDiariaResponse(
                hoje, 120, 0L, 0.0, false, 0, 0L, 100.0, true, false
        );
        when(progressoMetaDiariaSupport.calcularProgresso(eq(meta), eq(0L), eq(0L), eq(hoje))).thenReturn(progresso);
        when(progressoMetaDiariaSupport.isMetaDiariaCumprida(progresso)).thenReturn(false);

        when(ofensivaUsuarioRepository.save(ofensiva)).thenReturn(ofensiva);
        when(calculoOfensivaSupport.isEstudouHoje(eq(ofensiva), eq(hoje))).thenReturn(false);
        when(calculoOfensivaSupport.isOfensivaAtiva(eq(ofensiva), eq(hoje))).thenReturn(true);

        StatusOfensivaResponse response = ofensivaService.obterStatusOfensiva(usuario.getId());

        assertThat(response).isNotNull();
        assertThat(response.diasConsecutivosAtual()).isEqualTo(3);
        verify(usuarioValidator, atLeastOnce()).validarUsuarioAtivo(usuario);
        verify(ofensivaUsuarioRepository).save(ofensiva);
    }

    @Test
    @DisplayName("[obterStatusOfensiva] Deve lançar ForbiddenException quando usuário inativo")
    void deveLancarForbiddenAoConsultarComUsuarioInativo() {
        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        doThrow(new ForbiddenException("Usuário inativo ou bloqueado no sistema."))
                .when(usuarioValidator).validarUsuarioAtivo(usuario);

        assertThatThrownBy(() -> ofensivaService.obterStatusOfensiva(usuario.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Usuário inativo ou bloqueado no sistema.");
    }

    @Test
    @DisplayName("[registrarEstudo] Deve registrar avanço na ofensiva quando meta diária for cumprida")
    void deveRegistrarAvancoNaOfensivaQuandoMetaCumprida() {
        LocalDate hoje = LocalDate.now();
        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        when(ofensivaUsuarioFinder.buscarOuCriar(usuario)).thenReturn(ofensiva);

        TotalizadorSessaoProjection resumo = mock(TotalizadorSessaoProjection.class);
        when(resumo.getTotalSegundos()).thenReturn(7200L); // 120 min

        when(metaDiariaFinder.buscarOuCriar(usuario)).thenReturn(meta);
        when(sessaoEstudoRepository.obterDadosAgregados(eq(usuario.getId()), isNull(), any(Instant.class), any(Instant.class)))
                .thenReturn(resumo);
        when(tentativaQuestaoRepository.contarQuestoesRespondidasNoIntervalo(eq(usuario.getId()), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        ProgressoMetaDiariaResponse progresso = new ProgressoMetaDiariaResponse(
                hoje, 120, 120L, 100.0, true, 0, 0L, 100.0, true, true
        );
        when(progressoMetaDiariaSupport.calcularProgresso(eq(meta), eq(7200L), eq(0L), eq(hoje))).thenReturn(progresso);
        when(progressoMetaDiariaSupport.isMetaDiariaCumprida(progresso)).thenReturn(true);

        doNothing().when(calculoOfensivaSupport).registrarEstudo(ofensiva, hoje);
        when(ofensivaUsuarioRepository.save(ofensiva)).thenReturn(ofensiva);
        when(calculoOfensivaSupport.isEstudouHoje(eq(ofensiva), eq(hoje))).thenReturn(true);
        when(calculoOfensivaSupport.isOfensivaAtiva(eq(ofensiva), eq(hoje))).thenReturn(true);

        StatusOfensivaResponse response = ofensivaService.registrarEstudo(usuario.getId(), hoje);

        assertThat(response).isNotNull();
        assertThat(response.estudouHoje()).isTrue();
        verify(calculoOfensivaSupport).registrarEstudo(ofensiva, hoje);
        verify(ofensivaUsuarioRepository).save(ofensiva);
    }

    @Test
    @DisplayName("[registrarEstudoSilencioso] Não deve avançar ofensiva se meta diária não for batida")
    void naoDeveAvancarOfensivaSeMetaNaoBatida() {
        LocalDate hoje = LocalDate.now();
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        when(ofensivaUsuarioFinder.buscarOuCriar(usuario)).thenReturn(ofensiva);

        TotalizadorSessaoProjection resumo = mock(TotalizadorSessaoProjection.class);
        when(resumo.getTotalSegundos()).thenReturn(1800L); // 30 min (meta = 120)

        when(metaDiariaFinder.buscarOuCriar(usuario)).thenReturn(meta);
        when(sessaoEstudoRepository.obterDadosAgregados(eq(usuario.getId()), isNull(), any(Instant.class), any(Instant.class)))
                .thenReturn(resumo);
        when(tentativaQuestaoRepository.contarQuestoesRespondidasNoIntervalo(eq(usuario.getId()), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        ProgressoMetaDiariaResponse progresso = new ProgressoMetaDiariaResponse(
                hoje, 120, 30L, 25.0, false, 0, 0L, 100.0, true, false
        );
        when(progressoMetaDiariaSupport.calcularProgresso(eq(meta), eq(1800L), eq(0L), eq(hoje))).thenReturn(progresso);
        when(progressoMetaDiariaSupport.isMetaDiariaCumprida(progresso)).thenReturn(false);
        when(ofensivaUsuarioRepository.save(ofensiva)).thenReturn(ofensiva);

        ofensivaService.registrarEstudoSilencioso(usuario, hoje);

        verify(calculoOfensivaSupport, never()).registrarEstudo(any(), any());
        verify(ofensivaUsuarioRepository).save(ofensiva);
    }
}
