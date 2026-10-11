package com.kyofoundation.skillnapse.modules.flashcard.service;

import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.ai.dto.PromptRequest;
import com.kyofoundation.skillnapse.modules.ai.service.AiOrchestratorService;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.finder.UserFinder;
import com.kyofoundation.skillnapse.modules.auth.validator.UsuarioValidator;
import com.kyofoundation.skillnapse.modules.flashcard.dto.payload.FlashcardGeradoIaPayload;
import com.kyofoundation.skillnapse.modules.flashcard.dto.payload.LoteFlashcardsIaPayload;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.GerarFlashcardsIaRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.FlashcardResponse;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Baralho;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Flashcard;
import com.kyofoundation.skillnapse.modules.flashcard.repository.FlashcardRepository;
import com.kyofoundation.skillnapse.modules.flashcard.support.FlashcardBaralhoResolucaoSupport;
import com.kyofoundation.skillnapse.modules.flashcard.support.PromptFlashcardIaSupport;
import com.kyofoundation.skillnapse.modules.flashcard.validator.FlashcardIaValidator;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.MateriaFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.PlanoEstudoFinder;
import com.kyofoundation.skillnapse.modules.planoestudo.finder.TopicoFinder;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashcardIaServiceTest {

    @Mock
    private UserFinder userFinder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @Mock
    private PlanoEstudoFinder planoEstudoFinder;

    @Mock
    private MateriaFinder materiaFinder;

    @Mock
    private TopicoFinder topicoFinder;

    @Mock
    private FlashcardBaralhoResolucaoSupport flashcardBaralhoResolucaoSupport;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private FlashcardIaValidator flashcardIaValidator;

    @Mock
    private PromptFlashcardIaSupport promptFlashcardIaSupport;

    @Mock
    private AiOrchestratorService aiOrchestratorService;

    @InjectMocks
    private FlashcardIaService flashcardIaService;

    private Usuario usuario;
    private PlanoEstudo plano;
    private Materia materia;
    private Topico topico;
    private Baralho baralho;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Concurseiro Antigravity")
                .email("estudante@skillnapse.com")
                .ativo(true)
                .build();

        plano = PlanoEstudo.builder()
                .id(UUID.randomUUID())
                .titulo("Plano PMPE 2024")
                .usuario(usuario)
                .build();

        materia = Materia.builder()
                .id(UUID.randomUUID())
                .nome("Direito Constitucional")
                .planoEstudo(plano)
                .build();

        topico = Topico.builder()
                .id(UUID.randomUUID())
                .titulo("Direitos e Garantias Fundamentais")
                .materia(materia)
                .build();

        baralho = Baralho.builder()
                .id(UUID.randomUUID())
                .titulo("Flashcards - Direito Constitucional")
                .usuario(usuario)
                .materia(materia)
                .build();
    }

    @Test
    @DisplayName("Deve gerar e persistir flashcards por IA com sucesso")
    void deveGerarFlashcardsIaComSucesso() {
        GerarFlashcardsIaRequest request = new GerarFlashcardsIaRequest(
                plano.getId(),
                materia.getId(),
                topico.getId(),
                "Remédios Constitucionais",
                null,
                5,
                null
        );

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        doNothing().when(usuarioValidator).validarUsuarioAtivo(usuario);
        doNothing().when(flashcardIaValidator).validarRequisicao(request);

        when(planoEstudoFinder.findById(plano.getId())).thenReturn(plano);
        doNothing().when(flashcardIaValidator).validarPropriedadePlano(usuario, plano);

        when(materiaFinder.findById(materia.getId())).thenReturn(materia);
        when(topicoFinder.findById(topico.getId())).thenReturn(topico);
        when(flashcardBaralhoResolucaoSupport.resolverBaralhoDestino(usuario, materia, plano, null)).thenReturn(baralho);

        when(promptFlashcardIaSupport.construirPrompt(any(), any(), any(), any(), anyInt()))
                .thenReturn("Prompt estruturado de flashcards");

        LoteFlashcardsIaPayload payloadIa = new LoteFlashcardsIaPayload(List.of(
                new FlashcardGeradoIaPayload("O que é Habeas Data?", "Garante acesso ou retificação de informações relativas à pessoa do impetrante."),
                new FlashcardGeradoIaPayload("Qual o prazo do Mandado de Segurança?", "120 dias da ciência do ato.")
        ));

        when(aiOrchestratorService.generateStructured(any(PromptRequest.class), eq(LoteFlashcardsIaPayload.class)))
                .thenReturn(payloadIa);

        when(flashcardRepository.save(any(Flashcard.class))).thenAnswer(invocation -> {
            Flashcard f = invocation.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        List<FlashcardResponse> responses = flashcardIaService.gerarFlashcardsIa(usuario.getId(), request);

        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).frente()).isEqualTo("O que é Habeas Data?");
        assertThat(responses.get(1).frente()).isEqualTo("Qual o prazo do Mandado de Segurança?");

        verify(flashcardRepository, times(2)).save(any(Flashcard.class));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException se o plano pertencer a outro usuário")
    void deveLancarForbiddenQuandoPlanoPertencerAOutroUsuario() {
        GerarFlashcardsIaRequest request = new GerarFlashcardsIaRequest(
                plano.getId(),
                null,
                null,
                null,
                null,
                5,
                null
        );

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(planoEstudoFinder.findById(plano.getId())).thenReturn(plano);
        doThrow(new ForbiddenException("Você não tem permissão para gerar flashcards vinculados a este plano de estudos."))
                .when(flashcardIaValidator).validarPropriedadePlano(usuario, plano);

        assertThatThrownBy(() -> flashcardIaService.gerarFlashcardsIa(usuario.getId(), request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Você não tem permissão");

        verify(aiOrchestratorService, never()).generateStructured(any(), any());
    }

    @Test
    @DisplayName("Deve delegar resolução do baralho para FlashcardBaralhoResolucaoSupport")
    void deveDelegarResolucaoDeBaralhoParaSupport() {
        GerarFlashcardsIaRequest request = new GerarFlashcardsIaRequest(
                plano.getId(),
                materia.getId(),
                topico.getId(),
                null,
                null,
                2,
                null
        );

        when(userFinder.findById(usuario.getId())).thenReturn(usuario);
        when(planoEstudoFinder.findById(plano.getId())).thenReturn(plano);
        when(materiaFinder.findById(materia.getId())).thenReturn(materia);
        when(topicoFinder.findById(topico.getId())).thenReturn(topico);

        Baralho baralhoResolvido = Baralho.builder()
                .id(UUID.randomUUID())
                .titulo("Flashcards - Direito Constitucional")
                .usuario(usuario)
                .materia(materia)
                .build();
        when(flashcardBaralhoResolucaoSupport.resolverBaralhoDestino(usuario, materia, plano, null))
                .thenReturn(baralhoResolvido);

        LoteFlashcardsIaPayload payloadIa = new LoteFlashcardsIaPayload(List.of(
                new FlashcardGeradoIaPayload("Conceito X", "Definição Y")
        ));
        when(aiOrchestratorService.generateStructured(any(PromptRequest.class), eq(LoteFlashcardsIaPayload.class)))
                .thenReturn(payloadIa);

        when(flashcardRepository.save(any(Flashcard.class))).thenAnswer(invocation -> {
            Flashcard f = invocation.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        List<FlashcardResponse> responses = flashcardIaService.gerarFlashcardsIa(usuario.getId(), request);

        assertThat(responses).hasSize(1);
        verify(flashcardBaralhoResolucaoSupport).resolverBaralhoDestino(usuario, materia, plano, null);
    }
}
