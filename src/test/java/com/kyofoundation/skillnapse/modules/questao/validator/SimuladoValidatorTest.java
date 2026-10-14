package com.kyofoundation.skillnapse.modules.questao.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.questao.dto.request.CriarSimuladoRequest;
import com.kyofoundation.skillnapse.modules.questao.entity.Simulado;
import com.kyofoundation.skillnapse.modules.questao.enums.TipoSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimuladoValidatorTest {

    private SimuladoValidator validator;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        validator = new SimuladoValidator();
        usuario = Usuario.builder().id(UUID.randomUUID()).build();
    }

    @Test
    @DisplayName("Deve validar criação de simulado com sucesso")
    void deveValidarCriacaoComSucesso() {
        CriarSimuladoRequest request = new CriarSimuladoRequest("Simulado Geral", TipoSimulado.MANUAL);
        assertThatCode(() -> validator.validarCriacao(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lançar BadRequestException para request nulo")
    void deveLancarBadRequestParaRequestNulo() {
        assertThatThrownBy(() -> validator.validarCriacao(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Os dados de criação do simulado não podem ser nulos.");
    }

    @Test
    @DisplayName("Deve lançar BadRequestException para título em branco")
    void deveLancarBadRequestParaTituloEmBranco() {
        CriarSimuladoRequest request = new CriarSimuladoRequest("   ", TipoSimulado.MANUAL);
        assertThatThrownBy(() -> validator.validarCriacao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O título do simulado é obrigatório.");
    }

    @Test
    @DisplayName("Deve lançar BadRequestException para título muito longo")
    void deveLancarBadRequestParaTituloLongo() {
        CriarSimuladoRequest request = new CriarSimuladoRequest("A".repeat(301), TipoSimulado.MANUAL);
        assertThatThrownBy(() -> validator.validarCriacao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O título do simulado não pode ultrapassar 300 caracteres.");
    }

    @Test
    @DisplayName("Deve validar propriedade com sucesso")
    void deveValidarPropriedadeComSucesso() {
        Simulado simulado = Simulado.builder().id(UUID.randomUUID()).usuario(usuario).build();
        assertThatCode(() -> validator.validarPropriedade(usuario, simulado)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException para simulado de outro usuário")
    void deveLancarForbiddenParaOutroUsuario() {
        Usuario outro = Usuario.builder().id(UUID.randomUUID()).build();
        Simulado simulado = Simulado.builder().id(UUID.randomUUID()).usuario(outro).build();

        assertThatThrownBy(() -> validator.validarPropriedade(usuario, simulado))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Você não possui permissão para acessar ou modificar este simulado.");
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao concluir simulado já finalizado")
    void deveLancarBadRequestParaSimuladoJaConcluido() {
        Simulado simulado = Simulado.builder().id(UUID.randomUUID()).usuario(usuario).concluido(true).build();

        assertThatThrownBy(() -> validator.validarConclusao(simulado))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O simulado já foi concluído anteriormente.");
    }
}
