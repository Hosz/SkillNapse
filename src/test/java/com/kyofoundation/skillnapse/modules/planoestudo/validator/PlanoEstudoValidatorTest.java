package com.kyofoundation.skillnapse.modules.planoestudo.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EdicaoPlanoEstudoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.PlanoEstudoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanoEstudoValidatorTest {

    private PlanoEstudoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PlanoEstudoValidator();
    }

    @Test
    @DisplayName("Deve validar criacao de plano com sucesso")
    void deveValidarCriacaoComSucesso() {
        PlanoEstudoRequest request = new PlanoEstudoRequest("Concurso Magistratura", "Meta 2026");
        assertThatCode(() -> validator.validarCriacao(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve validar criacao de plano com descricao nula ou vazia sem erro")
    void deveValidarCriacaoComDescricaoOpcional() {
        PlanoEstudoRequest requestSemDescricao = new PlanoEstudoRequest("Concurso Magistratura", null);
        PlanoEstudoRequest requestDescricaoVazia = new PlanoEstudoRequest("Concurso Magistratura", "");

        assertThatCode(() -> validator.validarCriacao(requestSemDescricao)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validarCriacao(requestDescricaoVazia)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lancar BadRequestException quando titulo for nulo ou vazio")
    void deveLancarExcecaoQuandoTituloVazio() {
        PlanoEstudoRequest requestNulo = new PlanoEstudoRequest(null, "Descricao");
        PlanoEstudoRequest requestVazio = new PlanoEstudoRequest("   ", "Descricao");

        assertThatThrownBy(() -> validator.validarCriacao(requestNulo))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O título não pode ser vazio.");

        assertThatThrownBy(() -> validator.validarCriacao(requestVazio))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O título não pode ser vazio.");
    }

    @Test
    @DisplayName("Deve validar edicao com sucesso")
    void deveValidarEdicaoComSucesso() {
        EdicaoPlanoEstudoRequest request = new EdicaoPlanoEstudoRequest("Novo Titulo", "Nova Descricao", true);
        assertThatCode(() -> validator.validarEdicao(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lancar BadRequestException quando nenhum campo for informado na edicao")
    void deveLancarExcecaoQuandoEdicaoSemCampos() {
        EdicaoPlanoEstudoRequest request = new EdicaoPlanoEstudoRequest(null, null, null);

        assertThatThrownBy(() -> validator.validarEdicao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Pelo menos um campo deve ser informado para atualização.");
    }

    @Test
    @DisplayName("Deve lancar ForbiddenException quando plano pertencer a outro usuario")
    void deveLancarForbiddenExceptionQuandoPlanoDeOutroUsuario() {
        Usuario usuario1 = Usuario.builder().id(UUID.randomUUID()).build();
        Usuario usuario2 = Usuario.builder().id(UUID.randomUUID()).build();
        PlanoEstudo plano = PlanoEstudo.builder().usuario(usuario2).build();

        assertThatThrownBy(() -> validator.validarPlanoPertenceUsuario(usuario1, plano))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("O plano de estudo não pertence à esse usuário.");
    }
}
