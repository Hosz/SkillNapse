package com.kyofoundation.skillnapse.modules.sessao.service;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.gamificacao.service.OfensivaService;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.sessao.dto.request.RegistrarSessaoEstudoRequest;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.ResumoHorasLiquidasResponse;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.SessaoEstudoResponse;
import com.kyofoundation.skillnapse.modules.sessao.entity.SessaoEstudo;
import com.kyofoundation.skillnapse.modules.sessao.enums.StatusSessaoEstudo;
import com.kyofoundation.skillnapse.modules.sessao.finder.SessaoEstudoFinder;
import com.kyofoundation.skillnapse.modules.sessao.repository.SessaoEstudoRepository;
import com.kyofoundation.skillnapse.modules.sessao.repository.TotalizadorSessaoProjection;
import com.kyofoundation.skillnapse.modules.sessao.support.ResumoHorasLiquidasSupport;
import com.kyofoundation.skillnapse.modules.sessao.validator.SessaoEstudoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessaoEstudoServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private TopicoFinder topicoFinder;

    @Mock
    private SessaoEstudoFinder sessaoEstudoFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private SessaoEstudoValidator sessaoEstudoValidator;

    @Mock
    private SessaoEstudoRepository sessaoEstudoRepository;

    @Mock
    private ResumoHorasLiquidasSupport resumoHorasLiquidasSupport;

    @Mock
    private OfensivaService ofensivaService;

    @InjectMocks
    private SessaoEstudoService service;

    private Usuario usuario;
    private Topico topico;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(UUID.randomUUID()).email("estudante@skillnapse.com").build();
        Materia materia = Materia.builder().id(UUID.randomUUID()).nome("Física").build();
        topico = Topico.builder().id(UUID.randomUUID()).titulo("Cinemática").materia(materia).build();
    }

    @Test
    @DisplayName("Deve registrar sessao de estudo com sucesso")
    void deveRegistrarSessaoComSucesso() {
        Instant agora = Instant.now();
        RegistrarSessaoEstudoRequest request = new RegistrarSessaoEstudoRequest(
                topico.getId(),
                agora.minusSeconds(1800),
                agora,
                1500,
                StatusSessaoEstudo.CONCLUIDA,
                "Excelente foco"
        );

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(topicoFinder.findById(request.topicoId())).thenReturn(topico);
        when(sessaoEstudoRepository.save(any(SessaoEstudo.class))).thenAnswer(invocation -> {
            SessaoEstudo s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SessaoEstudoResponse response = service.registrarSessaoEstudo(usuario.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.topicoId()).isEqualTo(topico.getId());
        assertThat(response.duracaoLiquidaSegundos()).isEqualTo(1500);

        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(sessaoEstudoValidator).validarRegistro(request, usuario, topico);
        verify(sessaoEstudoRepository).save(any(SessaoEstudo.class));
    }

    @Test
    @DisplayName("Deve listar historico paginado com filtros")
    void deveListarHistoricoPaginadoComFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        SessaoEstudo sessao = SessaoEstudo.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .topico(topico)
                .duracaoLiquidaSegundos(1800)
                .build();

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(topicoFinder.findById(topico.getId())).thenReturn(topico);
        when(sessaoEstudoFinder.buscarComFiltros(eq(usuario.getId()), eq(topico.getId()), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sessao)));

        Page<SessaoEstudoResponse> pagina = service.listarHistoricoSessoesEstudo(usuario.getId(), topico.getId(), null, null, pageable);

        assertThat(pagina).isNotEmpty();
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).topicoTitulo()).isEqualTo("Cinemática");

        verify(sessaoEstudoValidator).validarTopicoPertenceUsuario(usuario, topico);
    }

    @Test
    @DisplayName("Deve visualizar sessao por id com sucesso")
    void deveVisualizarSessaoPorId() {
        UUID sessaoId = UUID.randomUUID();
        SessaoEstudo sessao = SessaoEstudo.builder()
                .id(sessaoId)
                .usuario(usuario)
                .topico(topico)
                .build();

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(sessaoEstudoFinder.findById(sessaoId)).thenReturn(sessao);

        SessaoEstudoResponse response = service.visualizarSessaoEstudo(usuario.getId(), sessaoId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(sessaoId);

        verify(sessaoEstudoValidator).validarPropriedadeSessao(usuario, sessao);
    }

    @Test
    @DisplayName("Deve obter resumo de horas liquidas com sucesso")
    void deveObterResumoHorasLiquidas() {
        TotalizadorSessaoProjection projection = mock(TotalizadorSessaoProjection.class);
        ResumoHorasLiquidasResponse resumoEsperado = new ResumoHorasLiquidasResponse(3600L, 1.0, 1L, 0L, 1L);

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(sessaoEstudoRepository.obterDadosAgregados(usuario.getId(), null, null, null)).thenReturn(projection);
        when(resumoHorasLiquidasSupport.calcularResumo(projection)).thenReturn(resumoEsperado);

        ResumoHorasLiquidasResponse resultado = service.obterResumoHoras(usuario.getId(), null, null, null);

        assertThat(resultado).isEqualTo(resumoEsperado);
        verify(sessaoEstudoValidator).validarFiltroPeriodo(null, null);
    }

    @Test
    @DisplayName("Deve obter resumo de horas de hoje com sucesso")
    void deveObterResumoHojeComSucesso() {
        TotalizadorSessaoProjection projection = mock(TotalizadorSessaoProjection.class);
        ResumoHorasLiquidasResponse resumoEsperado = new ResumoHorasLiquidasResponse(1800L, 0.5, 1L, 0L, 1L);

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(resumoHorasLiquidasSupport.resolverZoneId("America/Sao_Paulo")).thenReturn(java.time.ZoneId.of("America/Sao_Paulo"));
        when(sessaoEstudoRepository.obterDadosAgregados(eq(usuario.getId()), isNull(), any(Instant.class), any(Instant.class)))
                .thenReturn(projection);
        when(resumoHorasLiquidasSupport.calcularResumo(projection)).thenReturn(resumoEsperado);

        ResumoHorasLiquidasResponse resultado = service.obterResumoHoje(usuario.getId(), "America/Sao_Paulo");

        assertThat(resultado).isEqualTo(resumoEsperado);
    }

    @Test
    @DisplayName("Deve apagar sessao de estudo com sucesso")
    void deveApagarSessaoComSucesso() {
        UUID sessaoId = UUID.randomUUID();
        SessaoEstudo sessao = SessaoEstudo.builder()
                .id(sessaoId)
                .usuario(usuario)
                .build();

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(sessaoEstudoFinder.findById(sessaoId)).thenReturn(sessao);

        service.apagarSessaoEstudo(usuario.getId(), sessaoId);

        verify(sessaoEstudoValidator).validarPropriedadeSessao(usuario, sessao);
        verify(sessaoEstudoRepository).delete(sessao);
    }
}
