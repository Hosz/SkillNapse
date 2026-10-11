package com.kyofoundation.skillnapse.modules.questao.support;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import com.kyofoundation.skillnapse.modules.questao.dto.projection.MetricasTopicoProjection;
import com.kyofoundation.skillnapse.modules.questao.entity.TentativaQuestao;
import com.kyofoundation.skillnapse.modules.questao.finder.TentativaQuestaoFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProficienciaTopicoSupportTest {

    @Mock
    private TopicoRepository topicoRepository;

    @Mock
    private TentativaQuestaoFinder tentativaQuestaoFinder;

    @InjectMocks
    private ProficienciaTopicoSupport support;

    @Test
    @DisplayName("Deve calcular corretamente os níveis de proficiência de acordo com a taxa de acertos")
    void deveCalcularNiveisDeProficiencia() {
        assertThat(support.calcularNivelProficiencia(0, 0)).isEqualTo(NivelProficiencia.INICIANTE);
        assertThat(support.calcularNivelProficiencia(10, 9)).isEqualTo(NivelProficiencia.AVANCADO);
        assertThat(support.calcularNivelProficiencia(10, 8)).isEqualTo(NivelProficiencia.AVANCADO);
        assertThat(support.calcularNivelProficiencia(10, 6)).isEqualTo(NivelProficiencia.INTERMEDIARIO);
        assertThat(support.calcularNivelProficiencia(10, 5)).isEqualTo(NivelProficiencia.INTERMEDIARIO);
        assertThat(support.calcularNivelProficiencia(10, 4)).isEqualTo(NivelProficiencia.INICIANTE);
    }

    @Test
    @DisplayName("Deve atualizar proficiência do tópico com base nas métricas consolidadas")
    void deveAtualizarProficienciaDoTopico() {
        Usuario usuario = Usuario.builder().id(UUID.randomUUID()).build();
        Topico topico = Topico.builder()
                .id(UUID.randomUUID())
                .nivelProficiencia(NivelProficiencia.INICIANTE)
                .build();

        MetricasTopicoProjection metricas = mock(MetricasTopicoProjection.class);
        when(metricas.getTotalTentativas()).thenReturn(10L);
        when(metricas.getTotalAcertos()).thenReturn(8L);

        when(tentativaQuestaoFinder.obterMetricasPorTopicos(usuario, List.of(topico.getId())))
                .thenReturn(List.of(metricas));

        support.atualizarProficienciaTopico(usuario, topico);

        assertThat(topico.getNivelProficiencia()).isEqualTo(NivelProficiencia.AVANCADO);
        verify(topicoRepository).save(topico);
    }

    @Test
    @DisplayName("Não deve atualizar se usuário ou tópico for nulo")
    void naoDeveAtualizarComParametrosNulos() {
        support.atualizarProficienciaTopico(null, null);
        verify(topicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar proficiência dos tópicos a partir das tentativas de um simulado")
    void deveAtualizarProficienciaTopicosDoSimulado() {
        UUID simuladoId = UUID.randomUUID();

        Topico topico1 = Topico.builder()
                .id(UUID.randomUUID())
                .nivelProficiencia(NivelProficiencia.INICIANTE)
                .build();

        TentativaQuestao t1 = TentativaQuestao.builder().topico(topico1).acertou(true).build();
        TentativaQuestao t2 = TentativaQuestao.builder().topico(topico1).acertou(true).build();
        TentativaQuestao t3 = TentativaQuestao.builder().topico(topico1).acertou(false).build();
        TentativaQuestao t4 = TentativaQuestao.builder().topico(topico1).acertou(true).build();
        TentativaQuestao t5 = TentativaQuestao.builder().topico(topico1).acertou(true).build();
        // 4 acertos em 5 tentativas = 80% -> AVANCADO

        when(tentativaQuestaoFinder.buscarPorSimulado(simuladoId)).thenReturn(List.of(t1, t2, t3, t4, t5));

        support.atualizarProficienciaTopicosDoSimulado(simuladoId);

        assertThat(topico1.getNivelProficiencia()).isEqualTo(NivelProficiencia.AVANCADO);
        verify(topicoRepository).save(topico1);
    }
}
