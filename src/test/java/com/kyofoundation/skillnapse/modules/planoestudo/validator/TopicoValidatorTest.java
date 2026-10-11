package com.kyofoundation.skillnapse.modules.planoestudo.validator;

import com.kyofoundation.skillnapse.common.exception.BadRequestException;
import com.kyofoundation.skillnapse.common.exception.ForbiddenException;
import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.AtualizarProgressoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.CriarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.dto.request.EditarTopicoRequest;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopicoValidatorTest {

    private TopicoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TopicoValidator();
    }

    @Test
    @DisplayName("Deve validar criacao de topico com sucesso")
    void deveValidarCriacaoComSucesso() {
        CriarTopicoRequest request = new CriarTopicoRequest("Atos Administrativos", null, 3, NivelProficiencia.INTERMEDIARIO, 1);
        assertThatCode(() -> validator.validarCriacao(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lancar BadRequestException quando titulo for nulo ou vazio")
    void deveLancarExcecaoQuandoTituloVazio() {
        CriarTopicoRequest requestNulo = new CriarTopicoRequest(null, null, 1, NivelProficiencia.INICIANTE, 0);
        CriarTopicoRequest requestVazio = new CriarTopicoRequest("   ", null, 1, NivelProficiencia.INICIANTE, 0);

        assertThatThrownBy(() -> validator.validarCriacao(requestNulo))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O título do tópico é obrigatório.");

        assertThatThrownBy(() -> validator.validarCriacao(requestVazio))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O título do tópico é obrigatório.");
    }

    @Test
    @DisplayName("Deve aceitar titulo longo com ate 1000 caracteres e rejeitar acima de 1000 caracteres")
    void deveValidarLimite1000CaracteresTitulo() {
        String titulo1000 = "A".repeat(1000);
        String titulo1001 = "A".repeat(1001);

        CriarTopicoRequest valido = new CriarTopicoRequest(titulo1000, null, 1, NivelProficiencia.INICIANTE, 0);
        CriarTopicoRequest invalido = new CriarTopicoRequest(titulo1001, null, 1, NivelProficiencia.INICIANTE, 0);

        assertThatCode(() -> validator.validarCriacao(valido)).doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validarCriacao(invalido))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O título do tópico não pode ter mais de 1000 caracteres.");
    }

    @Test
    @DisplayName("Deve lancar BadRequestException quando peso do edital for menor que 1 ou maior que 10")
    void deveLancarExcecaoQuandoPesoInvalido() {
        CriarTopicoRequest pesoMenor = new CriarTopicoRequest("Topico", null, 0, NivelProficiencia.INICIANTE, 0);
        CriarTopicoRequest pesoMaior = new CriarTopicoRequest("Topico", null, 11, NivelProficiencia.INICIANTE, 0);

        assertThatThrownBy(() -> validator.validarCriacao(pesoMenor))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O peso no edital deve ser um valor entre 1 e 10.");

        assertThatThrownBy(() -> validator.validarCriacao(pesoMaior))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O peso no edital deve ser um valor entre 1 e 10.");
    }

    @Test
    @DisplayName("Deve lancar BadRequestException quando ordem for negativa")
    void deveLancarExcecaoQuandoOrdemNegativa() {
        CriarTopicoRequest ordemNegativa = new CriarTopicoRequest("Topico", null, 1, NivelProficiencia.INICIANTE, -1);

        assertThatThrownBy(() -> validator.validarCriacao(ordemNegativa))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A ordem do tópico não pode ser negativa.");
    }

    @Test
    @DisplayName("Deve validar edicao com sucesso")
    void deveValidarEdicaoComSucesso() {
        EditarTopicoRequest request = new EditarTopicoRequest("Novo Titulo", 2, NivelProficiencia.AVANCADO, true, 2);
        assertThatCode(() -> validator.validarEdicao(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lancar BadRequestException quando nenhum campo for informado na edicao")
    void deveLancarExcecaoQuandoEdicaoSemCampos() {
        EditarTopicoRequest request = new EditarTopicoRequest(null, null, null, null, null);

        assertThatThrownBy(() -> validator.validarEdicao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Pelo menos um campo deve ser informado para atualização.");
    }

    @Test
    @DisplayName("Deve validar atualizacao de progresso com sucesso")
    void deveValidarProgressoComSucesso() {
        AtualizarProgressoRequest request = new AtualizarProgressoRequest(true, NivelProficiencia.AVANCADO);
        assertThatCode(() -> validator.validarProgresso(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lancar BadRequestException quando nenhum dado de progresso for informado")
    void deveLancarExcecaoQuandoProgressoVazio() {
        AtualizarProgressoRequest request = new AtualizarProgressoRequest(null, null);

        assertThatThrownBy(() -> validator.validarProgresso(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Pelo menos o status de conclusão ou o nível de proficiência deve ser informado.");
    }

    @Test
    @DisplayName("Deve validar pertencimento do topico ao usuario com sucesso")
    void deveValidarTopicoPertenceUsuarioComSucesso() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(usuarioId).build();
        PlanoEstudo plano = PlanoEstudo.builder().usuario(usuario).build();
        Materia materia = Materia.builder().planoEstudo(plano).build();
        Topico topico = Topico.builder().materia(materia).build();

        assertThatCode(() -> validator.validarTopicoPertenceUsuario(usuario, topico))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lancar ForbiddenException quando topico pertencer a outro usuario")
    void deveLancarForbiddenExceptionQuandoTopicoDeOutroUsuario() {
        Usuario usuario1 = Usuario.builder().id(UUID.randomUUID()).build();
        Usuario usuario2 = Usuario.builder().id(UUID.randomUUID()).build();
        PlanoEstudo plano = PlanoEstudo.builder().usuario(usuario2).build();
        Materia materia = Materia.builder().planoEstudo(plano).build();
        Topico topico = Topico.builder().materia(materia).build();

        assertThatThrownBy(() -> validator.validarTopicoPertenceUsuario(usuario1, topico))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Você não tem permissão para acessar este tópico.");
    }

    @Test
    @DisplayName("Deve lancar BadRequestException quando topico pai pertencer a outra materia")
    void deveLancarExcecaoQuandoTopicoPaiDeOutraMateria() {
        UUID materia1Id = UUID.randomUUID();
        UUID materia2Id = UUID.randomUUID();

        Materia materia1 = Materia.builder().id(materia1Id).build();
        Materia materia2 = Materia.builder().id(materia2Id).build();

        Topico topicoPai = Topico.builder().materia(materia2).build();

        assertThatThrownBy(() -> validator.validarTopicoPaiPertenceMateria(topicoPai, materia1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O tópico pai deve pertencer à mesma matéria.");
    }
}
