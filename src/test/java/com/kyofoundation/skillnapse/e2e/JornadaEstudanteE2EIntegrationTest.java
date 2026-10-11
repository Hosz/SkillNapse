package com.kyofoundation.skillnapse.e2e;

import com.kyofoundation.skillnapse.TestcontainersConfiguration;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.auth.repository.UsuarioRepository;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.CriarBlocoHorarioRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.request.CriarTemplateSemanalRequest;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.BlocoHorarioResponse;
import com.kyofoundation.skillnapse.modules.cronograma.dto.response.TemplateSemanalResponse;
import com.kyofoundation.skillnapse.modules.cronograma.enums.DiaSemana;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import com.kyofoundation.skillnapse.modules.cronograma.service.BlocoHorarioTemplateService;
import com.kyofoundation.skillnapse.modules.cronograma.service.TemplateSemanalService;
import com.kyofoundation.skillnapse.modules.desempenho.dto.response.RelatorioLacunasResponse;
import com.kyofoundation.skillnapse.modules.desempenho.service.DesempenhoService;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarMateriaRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.PlanoEstudoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.MateriaResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.PlanoEstudoResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.response.TopicoResponse;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.PlanoEstudoRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.service.MateriaService;
import com.kyofoundation.skillnapse.modules.planoestudo.service.PlanoEstudoService;
import com.kyofoundation.skillnapse.modules.planoestudo.service.TopicoService;
import com.kyofoundation.skillnapse.modules.canvas.dto.request.SalvarRascunhoCanvasRequest;
import com.kyofoundation.skillnapse.modules.canvas.dto.response.RascunhoCanvasResponse;
import com.kyofoundation.skillnapse.modules.canvas.service.RascunhoCanvasService;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.CriarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.request.RevisarFlashcardRequest;
import com.kyofoundation.skillnapse.modules.flashcard.dto.response.FlashcardResponse;
import com.kyofoundation.skillnapse.modules.flashcard.entity.Baralho;
import com.kyofoundation.skillnapse.modules.flashcard.enums.ClassificacaoResposta;
import com.kyofoundation.skillnapse.modules.flashcard.repository.BaralhoRepository;
import com.kyofoundation.skillnapse.modules.flashcard.service.FlashcardService;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarAlternativaRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarQuestaoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarSimuladoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.request.ResponderQuestaoRequest;
import com.kyofoundation.skillnapse.modules.questao.dto.response.QuestaoDetalheResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.ResultadoResolucaoResponse;
import com.kyofoundation.skillnapse.modules.questao.dto.response.SimuladoResponse;
import com.kyofoundation.skillnapse.modules.questao.enums.DificuldadeQuestao;
import com.kyofoundation.skillnapse.modules.questao.enums.TipoSimulado;
import com.kyofoundation.skillnapse.modules.questao.service.QuestaoService;
import com.kyofoundation.skillnapse.modules.questao.service.ResolucaoQuestaoService;
import com.kyofoundation.skillnapse.modules.questao.service.SimuladoService;
import com.kyofoundation.skillnapse.modules.redacao.entity.SubmissaoRedacao;
import com.kyofoundation.skillnapse.modules.redacao.entity.TemaRedacao;
import com.kyofoundation.skillnapse.modules.redacao.repository.SubmissaoRedacaoRepository;
import com.kyofoundation.skillnapse.modules.redacao.repository.TemaRedacaoRepository;
import com.kyofoundation.skillnapse.modules.sessao.dto.request.RegistrarSessaoEstudoRequest;
import com.kyofoundation.skillnapse.modules.sessao.dto.response.SessaoEstudoResponse;
import com.kyofoundation.skillnapse.modules.sessao.enums.StatusSessaoEstudo;
import com.kyofoundation.skillnapse.modules.sessao.service.SessaoEstudoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JornadaEstudanteE2EIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlanoEstudoService planoEstudoService;

    @Autowired
    private PlanoEstudoRepository planoEstudoRepository;

    @Autowired
    private MateriaService materiaService;

    @Autowired
    private TopicoService topicoService;

    @Autowired
    private TemplateSemanalService templateSemanalService;

    @Autowired
    private BlocoHorarioTemplateService blocoHorarioTemplateService;

    @Autowired
    private SessaoEstudoService sessaoEstudoService;

    @Autowired
    private RascunhoCanvasService rascunhoCanvasService;

    @Autowired
    private FlashcardService flashcardService;

    @Autowired
    private BaralhoRepository baralhoRepository;

    @Autowired
    private QuestaoService questaoService;

    @Autowired
    private ResolucaoQuestaoService resolucaoQuestaoService;

    @Autowired
    private SimuladoService simuladoService;

    @Autowired
    private TemaRedacaoRepository temaRedacaoRepository;

    @Autowired
    private SubmissaoRedacaoRepository submissaoRedacaoRepository;

    @Autowired
    private DesempenhoService desempenhoService;

    @Test
    @DisplayName("Deve executar ciclo completo de vida do estudante de ponta a ponta com banco real no Testcontainers")
    void deveExecutarCicloCompletoDeEstudoE2EComSucesso() {
        // 1. Registro e persistência do estudante
        Usuario estudante = usuarioRepository.save(Usuario.builder()
                .nome("Concurseiro TCU")
                .email("estudante.tcu." + UUID.randomUUID() + "@skillnapse.com")
                .senhaHash(passwordEncoder.encode("senhaForte123"))
                .ativo(true)
                .build());

        UUID userId = estudante.getId();
        assertThat(userId).isNotNull();

        // 2. Criação do Plano de Estudos, Matéria e Tópico (escala peso 8)
        PlanoEstudoResponse plano = planoEstudoService.criarPlano(
                new PlanoEstudoRequest("Auditor Federal de Controle Externo", "Foco no concurso do TCU"),
                userId
        );
        assertThat(plano.id()).isNotNull();

        MateriaResponse materia = materiaService.criarMateria(
                userId,
                plano.id(),
                new CriarMateriaRequest("Direito Administrativo", "#0066CC", 1)
        );
        assertThat(materia.id()).isNotNull();

        TopicoResponse topico = topicoService.criarTopico(
                userId,
                materia.id(),
                new CriarTopicoRequest("Atos Administrativos", null, 8, NivelProficiencia.INTERMEDIARIO, 1)
        );
        assertThat(topico.id()).isNotNull();
        assertThat(topico.pesoEdital()).isEqualTo(8);

        // 3. Montagem do Cronograma Semanal com Bloco de Horário
        TemplateSemanalResponse template = templateSemanalService.criarTemplate(
                userId,
                new CriarTemplateSemanalRequest("Planejamento Semanal TCU", true)
        );

        BlocoHorarioResponse bloco = blocoHorarioTemplateService.adicionarBlocoHorario(
                userId,
                template.id(),
                new CriarBlocoHorarioRequest(
                        DiaSemana.SEGUNDA,
                        LocalTime.of(8, 0),
                        LocalTime.of(9, 0),
                        TipoBloco.FOCO_TEORIA,
                        materia.id(),
                        topico.id()
                )
        );
        assertThat(bloco.id()).isNotNull();
        assertThat(bloco.diaSemana()).isEqualTo(DiaSemana.SEGUNDA);

        // 4. Registro de Sessão de Estudo Líquida (50 min = 3000 segundos)
        Instant agora = Instant.now();
        SessaoEstudoResponse sessao = sessaoEstudoService.registrarSessaoEstudo(
                userId,
                new RegistrarSessaoEstudoRequest(
                        topico.id(),
                        agora.minus(50, ChronoUnit.MINUTES),
                        agora,
                        3000,
                        StatusSessaoEstudo.CONCLUIDA,
                        "Leitura e resolução de esquemas conceituais"
                )
        );
        assertThat(sessao.id()).isNotNull();
        assertThat(sessao.duracaoLiquidaSegundos()).isEqualTo(3000);

        // 5. Quadro de Rascunho Digital (Canvas Acoplado ao Tópico)
        RascunhoCanvasResponse canvasSalvo = rascunhoCanvasService.salvarRascunho(
                userId,
                topico.id(),
                new SalvarRascunhoCanvasRequest(topico.id(), "{\"strokes\":[{\"points\":[10,20,30,40]}]}")
        );
        assertThat(canvasSalvo.id()).isNotNull();
        assertThat(canvasSalvo.dadosDesenhoJson()).contains("strokes");

        // 6. Flashcards SRS e Revisão Ativa (Algoritmo SM-2)
        Baralho baralho = baralhoRepository.save(Baralho.builder()
                .usuario(estudante)
                .materia(null)
                .titulo("Baralho Atos Administrativos")
                .descricao("Flashcards de fixação de atos")
                .build());
        FlashcardResponse cardCriado = flashcardService.criarFlashcard(
                userId,
                new CriarFlashcardRequest(baralho.getId(), topico.id(), "O que é Motivo?", "Pressuposto fático e jurídico do ato.")
        );
        assertThat(cardCriado.id()).isNotNull();

        FlashcardResponse cardRevisado = flashcardService.revisarFlashcard(
                userId,
                cardCriado.id(),
                new RevisarFlashcardRequest(ClassificacaoResposta.BOM, 12)
        );
        assertThat(cardRevisado.intervaloDias()).isGreaterThan(0);
        assertThat(cardRevisado.repeticoes()).isEqualTo(1);

        // 7. Simulado e Resolução de Questões com Recálculo de Proficiência
        QuestaoDetalheResponse questao = questaoService.criar(
                userId,
                new CriarQuestaoRequest(
                        "Direito Administrativo",
                        "Atos Administrativos",
                        "Qual atributo do ato administrativo permite a execução direta pela Administração?",
                        "Explicacao sobre autoexecutoriedade.",
                        DificuldadeQuestao.MEDIA,
                        "Cebraspe",
                        2025,
                        List.of(
                                new CriarAlternativaRequest("A", "Presunção de legitimidade", false),
                                new CriarAlternativaRequest("B", "Autoexecutoriedade", true),
                                new CriarAlternativaRequest("C", "Tipicidade", false)
                        )
                )
        );

        UUID alternativaCorretaId = questao.alternativas().stream()
                .filter(a -> "Autoexecutoriedade".equals(a.texto()))
                .findFirst()
                .orElseThrow()
                .id();

        SimuladoResponse simulado = simuladoService.criar(
                new CriarSimuladoRequest("Simulado TCU - Bloco 1", TipoSimulado.MANUAL),
                userId
        );
        assertThat(simulado.id()).isNotNull();

        ResultadoResolucaoResponse resolucao = resolucaoQuestaoService.responder(
                questao.id(),
                new ResponderQuestaoRequest(alternativaCorretaId, 45, topico.id(), simulado.id()),
                userId
        );
        assertThat(resolucao.acertou()).isTrue();
        assertThat(resolucao.tempoGastoSegundos()).isEqualTo(45);

        SimuladoResponse simuladoConcluido = simuladoService.concluir(simulado.id(), userId);
        assertThat(simuladoConcluido.concluido()).isTrue();
        assertThat(simuladoConcluido.percentualAcerto()).isEqualTo(100.0);

        // 8. Laboratório de Redação com Submissão Persistida (escala 0 a 1000)
        PlanoEstudo planoEntity = planoEstudoRepository.findById(plano.id()).orElseThrow();
        TemaRedacao temaEntity = temaRedacaoRepository.save(TemaRedacao.builder()
                .planoEstudo(planoEntity)
                .titulo("O Papel do Controle Externo na Era Digital")
                .textosMotivadores("Texto motivador sobre auditoria algorítmica e inteligência artificial.")
                .criteriosAvaliacao("Dissertação argumentativa de 30 linhas com proposta de controle preventivo.")
                .build());

        SubmissaoRedacao submissaoSalva = submissaoRedacaoRepository.save(SubmissaoRedacao.builder()
                .usuario(estudante)
                .temaRedacao(temaEntity)
                .textoAluno("O controle externo desempenha papel crucial na preservação do interesse público...".repeat(8))
                .notaGeral(new BigDecimal("940.00"))
                .feedbackIaJson("{\"notaGeral\":940.0,\"comentariosGerais\":\"Texto exemplar com excelente domínio das competências.\"}")
                .criadoEm(Instant.now())
                .corrigidoEm(Instant.now())
                .build());
        assertThat(submissaoSalva.getId()).isNotNull();
        assertThat(submissaoSalva.getNotaGeral()).isEqualTo(new BigDecimal("940.00"));

        // 9. Consolidação Analítica de Desempenho e Mapeamento de Lacunas
        RelatorioLacunasResponse relatorio = desempenhoService.obterRelatorioLacunas(userId, plano.id());

        assertThat(relatorio).isNotNull();
        assertThat(relatorio.planoId()).isEqualTo(plano.id());
        assertThat(relatorio.totalQuestoesRespondidas()).isGreaterThanOrEqualTo(1L);
        assertThat(relatorio.taxaAcertoGeral()).isEqualTo(100.0);
        assertThat(relatorio.materias()).isNotEmpty();
    }
}
