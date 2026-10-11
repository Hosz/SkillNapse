package com.kyofoundation.skillnapse.modules.redacao.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.redacao.dto.payload.TemaRedacaoIaPayload;
import com.kyofoundation.skillnapse.modules.redacao.dto.request.GerarTemaRedacaoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemaRedacaoValidatorTest {

    private final TemaRedacaoValidator validator = new TemaRedacaoValidator();

    @Test
    @DisplayName("validarRequisicao deve lançar BadRequestException se request for nulo")
    void deveLancarSeRequestNulo() {
        assertThatThrownBy(() -> validator.validarRequisicao(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Os dados para geração do tema de redação não podem ser nulos.");
    }

    @Test
    @DisplayName("validarRequisicao deve lançar BadRequestException se planoEstudoId e eixoTematico forem nulos")
    void deveLancarSePlanoEstudoIdEEixoTematicoNulos() {
        GerarTemaRedacaoRequest request = new GerarTemaRedacaoRequest(null, null, null, "FGV", "Dissertativo", null);
        assertThatThrownBy(() -> validator.validarRequisicao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Informe ao menos o plano de estudos ou um eixo temático");
    }

    @Test
    @DisplayName("validarRequisicao deve passar se planoEstudoId for nulo mas eixoTematico for informado")
    void devePassarSeEixoTematicoInformadoSemPlano() {
        GerarTemaRedacaoRequest request = new GerarTemaRedacaoRequest(null, null, null, "FGV", "Dissertativo", "Segurança Pública");
        assertThatCode(() -> validator.validarRequisicao(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarRequisicao deve lançar BadRequestException se campos excederem 50 caracteres")
    void deveLancarSeCamposExcederemLimite() {
        UUID planoId = UUID.randomUUID();
        String textoLongo = "a".repeat(51);

        GerarTemaRedacaoRequest reqBancaLonga = new GerarTemaRedacaoRequest(planoId, null, null, textoLongo, null, null);
        assertThatThrownBy(() -> validator.validarRequisicao(reqBancaLonga))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("banca avaliadora não pode ultrapassar 50 caracteres");

        GerarTemaRedacaoRequest reqGeneroLongo = new GerarTemaRedacaoRequest(planoId, null, null, null, textoLongo, null);
        assertThatThrownBy(() -> validator.validarRequisicao(reqGeneroLongo))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("gênero textual não pode ultrapassar 50 caracteres");
    }

    @Test
    @DisplayName("validarRequisicao deve passar se dados forem válidos")
    void devePassarValidacaoRequisicaoValida() {
        GerarTemaRedacaoRequest request = new GerarTemaRedacaoRequest(UUID.randomUUID(), null, null, "FCC", "Dissertativo", null);
        assertThatCode(() -> validator.validarRequisicao(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarMateriaPertencePlano deve lançar ForbiddenException se matéria pertencer a outro plano")
    void deveLancarSeMateriaPertencerAOutroPlano() {
        PlanoEstudo plano1 = PlanoEstudo.builder().id(UUID.randomUUID()).build();
        PlanoEstudo plano2 = PlanoEstudo.builder().id(UUID.randomUUID()).build();
        Materia materia = Materia.builder().id(UUID.randomUUID()).planoEstudo(plano2).build();

        assertThatThrownBy(() -> validator.validarMateriaPertencePlano(materia, plano1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("A matéria informada não pertence ao plano de estudo indicado.");
    }

    @Test
    @DisplayName("validarTopicoPertencePlano deve lançar ForbiddenException se tópico pertencer a outro plano")
    void deveLancarSeTopicoPertencerAOutroPlano() {
        PlanoEstudo plano1 = PlanoEstudo.builder().id(UUID.randomUUID()).build();
        PlanoEstudo plano2 = PlanoEstudo.builder().id(UUID.randomUUID()).build();
        Materia materia = Materia.builder().id(UUID.randomUUID()).planoEstudo(plano2).build();
        Topico topico = Topico.builder().id(UUID.randomUUID()).materia(materia).build();

        assertThatThrownBy(() -> validator.validarTopicoPertencePlano(topico, plano1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("O tópico informado não pertence ao plano de estudo indicado.");
    }

    @Test
    @DisplayName("validarTopicoPertenceMateria deve lançar ForbiddenException se tópico pertencer a outra matéria")
    void deveLancarSeTopicoPertencerAOutraMateria() {
        Materia materia1 = Materia.builder().id(UUID.randomUUID()).build();
        Materia materia2 = Materia.builder().id(UUID.randomUUID()).build();
        Topico topico = Topico.builder().id(UUID.randomUUID()).materia(materia2).build();

        assertThatThrownBy(() -> validator.validarTopicoPertenceMateria(topico, materia1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("O tópico informado não pertence à matéria indicada.");
    }

    @Test
    @DisplayName("validarTemaGeradoIa deve lançar BadRequestException para payloads defeituosos")
    void deveLancarSePayloadIaInvalido() {
        assertThatThrownBy(() -> validator.validarTemaGeradoIa(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Não foi possível gerar a proposta");

        TemaRedacaoIaPayload tituloCurto = new TemaRedacaoIaPayload(
                "Curto",
                "Descrição válida do tema para teste.",
                "Textos motivadores adequados e longos com mais de 50 caracteres para teste de validação.",
                "Critérios de avaliação com extensão adequada."
        );
        assertThatThrownBy(() -> validator.validarTemaGeradoIa(tituloCurto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("título da redação deve ter entre 10 e 200 caracteres");

        TemaRedacaoIaPayload motivadoresCurtos = new TemaRedacaoIaPayload(
                "Título com extensão adequada de concurso",
                "Descrição válida do tema para teste.",
                "Texto curto",
                "Critérios de avaliação com extensão adequada."
        );
        assertThatThrownBy(() -> validator.validarTemaGeradoIa(motivadoresCurtos))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("textos motivadores gerados pela IA devem conter ao menos 50 caracteres");

        TemaRedacaoIaPayload criteriosCurtos = new TemaRedacaoIaPayload(
                "Título com extensão adequada de concurso",
                "Descrição válida do tema para teste.",
                "Textos motivadores adequados e longos com mais de 50 caracteres para teste de validação.",
                "Curto"
        );
        assertThatThrownBy(() -> validator.validarTemaGeradoIa(criteriosCurtos))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("critérios de avaliação da proposta devem conter ao menos 20 caracteres");
    }

    @Test
    @DisplayName("validarTemaGeradoIa deve passar se dados gerados forem robustos")
    void devePassarSeTemaGeradoValido() {
        TemaRedacaoIaPayload valido = new TemaRedacaoIaPayload(
                "Desafios da Transparência Pública na Era dos Algoritmos",
                "Contextualização sobre o uso de algoritmos no setor público.",
                "Texto I: A Lei de Acesso à Informação exige publicidade ativa. Texto II: Sistemas automatizados de decisão impactam o cidadão.",
                "Desenvolva texto dissertativo-argumentativo em até 30 linhas, abordando: 1) Dever de transparência; 2) Riscos de opacidade."
        );
        assertThatCode(() -> validator.validarTemaGeradoIa(valido)).doesNotThrowAnyException();
    }
}
