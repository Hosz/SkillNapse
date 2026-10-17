package com.kyofoundation.skillnapse.modules.questao.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.AlternativaGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.payload.QuestaoGeradaIaPayload;
import com.kyofoundation.skillnapse.modules.questao.dto.request.GerarSimuladoAdaptativoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimuladoAdaptativoValidatorTest {

    private final SimuladoAdaptativoValidator validator = new SimuladoAdaptativoValidator();

    @Test
    @DisplayName("validarRequisicao deve lançar BadRequestException quando request for nulo")
    void deveLancarQuandoRequestNulo() {
        assertThatThrownBy(() -> validator.validarRequisicao(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Os dados para geração do simulado adaptativo não podem ser nulos.");
    }

    @Test
    @DisplayName("validarRequisicao deve lançar BadRequestException quando planoEstudoId for nulo")
    void deveLancarQuandoPlanoEstudoIdNulo() {
        GerarSimuladoAdaptativoRequest request = new GerarSimuladoAdaptativoRequest(null, 5, "FCC", null);
        assertThatThrownBy(() -> validator.validarRequisicao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("O ID do plano de estudo é obrigatório.");
    }

    @Test
    @DisplayName("validarRequisicao deve lançar BadRequestException quando quantidade for menor que 3 ou maior que 90")
    void deveLancarQuandoQuantidadeForaDosLimites() {
        UUID planoId = UUID.randomUUID();
        GerarSimuladoAdaptativoRequest reqMenor = new GerarSimuladoAdaptativoRequest(planoId, 2, "FCC", null);
        GerarSimuladoAdaptativoRequest reqMaior = new GerarSimuladoAdaptativoRequest(planoId, 91, "FCC", null);

        assertThatThrownBy(() -> validator.validarRequisicao(reqMenor))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("entre 3 e 90");

        assertThatThrownBy(() -> validator.validarRequisicao(reqMaior))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("entre 3 e 90");
    }

    @Test
    @DisplayName("validarRequisicao deve passar com sucesso quando dados forem válidos")
    void devePassarValidacaoRequisicaoValida() {
        UUID planoId = UUID.randomUUID();
        GerarSimuladoAdaptativoRequest request = new GerarSimuladoAdaptativoRequest(planoId, 5, "FCC", null);

        assertThatCode(() -> validator.validarRequisicao(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarTopicosPertencemPlano deve lançar BadRequestException quando lista de tópicos estiver vazia")
    void deveLancarQuandoTopicosVazio() {
        PlanoEstudo plano = PlanoEstudo.builder().id(UUID.randomUUID()).build();

        assertThatThrownBy(() -> validator.validarTopicosPertencemPlano(Collections.emptyList(), plano))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Nenhum tópico válido foi encontrado");
    }

    @Test
    @DisplayName("validarTopicosPertencemPlano deve lançar ForbiddenException quando tópico pertencer a outro plano")
    void deveLancarQuandoTopicoDeOutroPlano() {
        PlanoEstudo plano1 = PlanoEstudo.builder().id(UUID.randomUUID()).build();
        PlanoEstudo plano2 = PlanoEstudo.builder().id(UUID.randomUUID()).build();

        Materia mat = Materia.builder().id(UUID.randomUUID()).planoEstudo(plano2).build();
        Topico topico = Topico.builder().id(UUID.randomUUID()).materia(mat).build();

        assertThatThrownBy(() -> validator.validarTopicosPertencemPlano(List.of(topico), plano1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("não pertencem ao plano de estudo informado");
    }

    @Test
    @DisplayName("validarTopicosPertencemPlano deve passar quando tópicos pertencerem ao plano")
    void devePassarTopicosDoMesmoPlano() {
        PlanoEstudo plano = PlanoEstudo.builder().id(UUID.randomUUID()).build();
        Materia mat = Materia.builder().id(UUID.randomUUID()).planoEstudo(plano).build();
        Topico topico = Topico.builder().id(UUID.randomUUID()).materia(mat).build();

        assertThatCode(() -> validator.validarTopicosPertencemPlano(List.of(topico), plano))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarQuestoesGeradasIa deve lançar BadRequestException se lista for nula ou vazia")
    void deveLancarQuandoQuestoesIaVazia() {
        assertThatThrownBy(() -> validator.validarQuestoesGeradasIa(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Não foi possível gerar questões com a IA");

        assertThatThrownBy(() -> validator.validarQuestoesGeradasIa(Collections.emptyList()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Não foi possível gerar questões com a IA");
    }

    @Test
    @DisplayName("validarQuestoesGeradasIa deve lançar BadRequestException se enunciado for em branco")
    void deveLancarQuandoEnunciadoEmBranco() {
        QuestaoGeradaIaPayload questao = new QuestaoGeradaIaPayload(
                "   ",
                "Explicação",
                "MEDIA",
                List.of(new AlternativaGeradaIaPayload("Opção A", true, 1), new AlternativaGeradaIaPayload("Opção B", false, 2))
        );

        assertThatThrownBy(() -> validator.validarQuestoesGeradasIa(List.of(questao)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("enunciado vazio");
    }

    @Test
    @DisplayName("validarQuestoesGeradasIa deve lançar BadRequestException se menos de 2 alternativas")
    void deveLancarQuandoMenosDeDuasAlternativas() {
        QuestaoGeradaIaPayload questao = new QuestaoGeradaIaPayload(
                "Enunciado da questão",
                "Explicação",
                "FACIL",
                List.of(new AlternativaGeradaIaPayload("Opção única", true, 1))
        );

        assertThatThrownBy(() -> validator.validarQuestoesGeradasIa(List.of(questao)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ao menos 2 alternativas");
    }

    @Test
    @DisplayName("validarQuestoesGeradasIa deve lançar BadRequestException se não tiver exatamente 1 correta")
    void deveLancarQuandoQuantidadeCorretasInvalida() {
        QuestaoGeradaIaPayload semCorreta = new QuestaoGeradaIaPayload(
                "Enunciado 1",
                "Explicação",
                "DIFICIL",
                List.of(new AlternativaGeradaIaPayload("Opção A", false, 1), new AlternativaGeradaIaPayload("Opção B", false, 2))
        );

        QuestaoGeradaIaPayload duasCorretas = new QuestaoGeradaIaPayload(
                "Enunciado 2",
                "Explicação",
                "MEDIA",
                List.of(new AlternativaGeradaIaPayload("Opção A", true, 1), new AlternativaGeradaIaPayload("Opção B", true, 2))
        );

        assertThatThrownBy(() -> validator.validarQuestoesGeradasIa(List.of(semCorreta)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exatamente uma alternativa correta");

        assertThatThrownBy(() -> validator.validarQuestoesGeradasIa(List.of(duasCorretas)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exatamente uma alternativa correta");
    }

    @Test
    @DisplayName("validarQuestoesGeradasIa deve passar com sucesso para questões bem estruturadas")
    void devePassarQuestoesIaValidas() {
        QuestaoGeradaIaPayload valida = new QuestaoGeradaIaPayload(
                "Enunciado válido de concurso",
                "Explicação detalhada",
                "MEDIA",
                List.of(
                        new AlternativaGeradaIaPayload("Opção A", false, 1),
                        new AlternativaGeradaIaPayload("Opção B", true, 2),
                        new AlternativaGeradaIaPayload("Opção C", false, 3),
                        new AlternativaGeradaIaPayload("Opção D", false, 4)
                )
        );

        assertThatCode(() -> validator.validarQuestoesGeradasIa(List.of(valida)))
                .doesNotThrowAnyException();
    }
}
