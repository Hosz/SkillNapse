package com.kyofoundation.skillnapse.modules.questao.service;

import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.gamificacao.service.OfensivaService;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
import com.kyofoundation.skillnapse.modules.questao.dto.request.ResponderQuestaoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.HistoricoTentativaResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.ResultadoResolucaoResponse;
import com.kyofoundation.skillnapse.modules.questao.entity.AlternativaQuestao;
import com.kyofoundation.skillnapse.modules.questao.entity.Questao;
import com.kyofoundation.skillnapse.modules.questao.entity.Simulado;
import com.kyofoundation.skillnapse.modules.questao.entity.TentativaQuestao;
import com.kyofoundation.skillnapse.modules.questao.finder.AlternativaQuestaoFinder;
import com.kyofoundation.skillnapse.modules.questao.finder.QuestaoFinder;
import com.kyofoundation.skillnapse.modules.questao.finder.SimuladoFinder;
import com.kyofoundation.skillnapse.modules.questao.finder.TentativaQuestaoFinder;
import com.kyofoundation.skillnapse.modules.questao.repository.TentativaQuestaoRepository;
import com.kyofoundation.skillnapse.modules.questao.validator.ResolucaoQuestaoValidator;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolucaoQuestaoServiceTest {

    @Mock
    private TentativaQuestaoRepository tentativaQuestaoRepository;

    @Mock
    private QuestaoFinder questaoFinder;

    @Mock
    private AlternativaQuestaoFinder alternativaQuestaoFinder;

    @Mock
    private SimuladoFinder simuladoFinder;

    @Mock
    private TopicoFinder topicoFinder;

    @Mock
    private com.kyofoundation.skillnapse.modules.questao.support.ProficienciaTopicoSupport proficienciaTopicoSupport;

    @Mock
    private TentativaQuestaoFinder tentativaQuestaoFinder;

    @Mock
    private UserFinder userFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private ResolucaoQuestaoValidator resolucaoQuestaoValidator;

    @Mock
    private OfensivaService ofensivaService;

    @InjectMocks
    private ResolucaoQuestaoService resolucaoQuestaoService;

    @Test
    @DisplayName("Deve responder questão com acerto validando usuário ativo e registrando ofensiva")
    void deveResponderQuestaoComAcerto() {
        UUID userId = UUID.randomUUID();
        UUID questaoId = UUID.randomUUID();
        UUID altId = UUID.randomUUID();

        Usuario usuario = Usuario.builder().id(userId).ativo(true).build();
        Questao questao = Questao.builder().id(questaoId).explicacaoGabarito("Explicação").build();
        AlternativaQuestao altCorreta = AlternativaQuestao.builder().id(altId).letra("A").correta(true).questao(questao).build();

        ResponderQuestaoRequest request = new ResponderQuestaoRequest(altId, 30, null, null);

        when(userFinder.findById(userId)).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        when(questaoFinder.findByIdComAlternativas(questaoId)).thenReturn(questao);
        when(alternativaQuestaoFinder.findById(altId)).thenReturn(altCorreta);
        doNothing().when(resolucaoQuestaoValidator).validarResolucao(questao, altCorreta, 30, null, null, usuario);
        when(alternativaQuestaoFinder.findCorretaPorQuestaoId(questaoId)).thenReturn(altCorreta);

        TentativaQuestao tentativaSalva = TentativaQuestao.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .questao(questao)
                .alternativaEscolhida(altCorreta)
                .acertou(true)
                .tempoGastoSegundos(30)
                .respondidoEm(Instant.now())
                .build();

        when(tentativaQuestaoRepository.save(any(TentativaQuestao.class))).thenReturn(tentativaSalva);
        doNothing().when(ofensivaService).registrarEstudoSilencioso(any(), any());

        ResultadoResolucaoResponse response = resolucaoQuestaoService.responder(questaoId, request, userId);

        assertThat(response).isNotNull();
        assertThat(response.acertou()).isTrue();
        assertThat(response.letraEscolhida()).isEqualTo("A");
        assertThat(response.letraCorreta()).isEqualTo("A");
        assertThat(response.explicacaoGabarito()).isEqualTo("Explicação");
        verify(usuarioValidator).validarUsuarioAtivo(usuario);
        verify(ofensivaService).registrarEstudoSilencioso(any(), any());
        verify(tentativaQuestaoRepository).save(any(TentativaQuestao.class));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException ao tentar responder com usuário inativo")
    void deveLancarForbiddenAoResponderComUsuarioInativo() {
        UUID userId = UUID.randomUUID();
        UUID questaoId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(false).build();
        ResponderQuestaoRequest request = new ResponderQuestaoRequest(UUID.randomUUID(), 30, null, null);

        when(userFinder.findById(userId)).thenReturn(usuario);
        doThrow(new ForbiddenException("Usuário inativo ou bloqueado no sistema."))
                .when(usuarioValidator).validarUsuarioAtivo(usuario);

        assertThatThrownBy(() -> resolucaoQuestaoService.responder(questaoId, request, userId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Usuário inativo ou bloqueado no sistema.");

        verify(tentativaQuestaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve responder questão com erro em simulado e tópico")
    void deveResponderQuestaoComErroVinculadaASimuladoETopico() {
        UUID userId = UUID.randomUUID();
        UUID questaoId = UUID.randomUUID();
        UUID altEscolhidaId = UUID.randomUUID();
        UUID altCorretaId = UUID.randomUUID();
        UUID simuladoId = UUID.randomUUID();
        UUID topicoId = UUID.randomUUID();

        Usuario usuario = Usuario.builder().id(userId).ativo(true).build();
        Questao questao = Questao.builder().id(questaoId).explicacaoGabarito("Explicação gabarito").build();
        AlternativaQuestao altEscolhida = AlternativaQuestao.builder().id(altEscolhidaId).letra("B").correta(false).questao(questao).build();
        AlternativaQuestao altCorreta = AlternativaQuestao.builder().id(altCorretaId).letra("C").correta(true).questao(questao).build();
        Simulado simulado = Simulado.builder().id(simuladoId).usuario(usuario).concluido(false).build();
        Topico topico = Topico.builder().id(topicoId).build();

        ResponderQuestaoRequest request = new ResponderQuestaoRequest(altEscolhidaId, 50, topicoId, simuladoId);

        when(userFinder.findById(userId)).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        when(questaoFinder.findByIdComAlternativas(questaoId)).thenReturn(questao);
        when(alternativaQuestaoFinder.findById(altEscolhidaId)).thenReturn(altEscolhida);
        when(simuladoFinder.findById(simuladoId)).thenReturn(simulado);
        when(topicoFinder.findById(topicoId)).thenReturn(topico);
        doNothing().when(resolucaoQuestaoValidator).validarResolucao(questao, altEscolhida, 50, simulado, topico, usuario);
        when(alternativaQuestaoFinder.findCorretaPorQuestaoId(questaoId)).thenReturn(altCorreta);

        TentativaQuestao tentativaSalva = TentativaQuestao.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .questao(questao)
                .alternativaEscolhida(altEscolhida)
                .acertou(false)
                .tempoGastoSegundos(50)
                .simulado(simulado)
                .topico(topico)
                .respondidoEm(Instant.now())
                .build();

        when(tentativaQuestaoRepository.save(any(TentativaQuestao.class))).thenReturn(tentativaSalva);
        doNothing().when(ofensivaService).registrarEstudoSilencioso(any(), any());

        ResultadoResolucaoResponse response = resolucaoQuestaoService.responder(questaoId, request, userId);

        assertThat(response).isNotNull();
        assertThat(response.acertou()).isFalse();
        assertThat(response.letraEscolhida()).isEqualTo("B");
        assertThat(response.letraCorreta()).isEqualTo("C");
    }

    @Test
    @DisplayName("Deve buscar histórico de tentativas do usuário ativo")
    void deveBuscarHistorico() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(userId).ativo(true).build();
        Pageable pageable = PageRequest.of(0, 10);

        Questao q = Questao.builder().id(UUID.randomUUID()).enunciado("Enunciado").build();
        AlternativaQuestao alt = AlternativaQuestao.builder().id(UUID.randomUUID()).letra("D").build();
        TentativaQuestao t = TentativaQuestao.builder()
                .id(UUID.randomUUID())
                .questao(q)
                .alternativaEscolhida(alt)
                .acertou(true)
                .respondidoEm(Instant.now())
                .build();
        Page<TentativaQuestao> pagina = new PageImpl<>(List.of(t));

        when(userFinder.findById(userId)).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        when(tentativaQuestaoFinder.buscarPorUsuario(usuario, true, pageable)).thenReturn(pagina);

        Page<HistoricoTentativaResponse> resultado = resolucaoQuestaoService.buscarHistorico(userId, true, pageable);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst().acertou()).isTrue();
        verify(usuarioValidator).validarUsuarioAtivo(usuario);
    }
}
