package com.kyofoundation.skillnapse.modules.edital.support;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.edital.dto.request.ConverterRascunhoEditalRequest;
import com.kyofoundation.skillnapse.modules.edital.dto.response.ConversaoEditalResponse;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalArvoreEstruturada;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalMaterialItem;
import com.kyofoundation.skillnapse.modules.edital.dto.structure.EditalTopicoItem;
import com.kyofoundation.skillnapse.modules.edital.entity.RascunhoEdital;
import com.kyofoundation.skillnapse.modules.edital.enums.StatusRascunhoEdital;
import com.kyofoundation.skillnapse.modules.edital.mapper.EditalTreeMapper;
import com.kyofoundation.skillnapse.modules.edital.repository.RascunhoEditalRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.MateriaRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.PlanoEstudoRepository;
import com.kyofoundation.skillnapse.modules.planoestudo.repository.TopicoRepository;
import org.junit.jupiter.api.BeforeEach;
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
class EditalConversaoSupportTest {

    @Mock
    private PlanoEstudoRepository planoEstudoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private TopicoRepository topicoRepository;

    @Mock
    private RascunhoEditalRepository rascunhoEditalRepository;

    @InjectMocks
    private EditalConversaoSupport support;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(UUID.randomUUID()).build();
    }

    @Test
    @DisplayName("Deve converter rascunho em plano de estudo com materias e topicos")
    void deveConverterRascunhoComSucesso() {
        EditalTopicoItem topicoItem = new EditalTopicoItem("Morfologia", 4, List.of("Substantivos", "Verbos"));
        EditalMaterialItem materiaItem = new EditalMaterialItem("Português", List.of(topicoItem));
        EditalArvoreEstruturada arvore = new EditalArvoreEstruturada("Concurso Caixa", "Técnico Bancário", List.of(materiaItem));

        String json = EditalTreeMapper.toJson(arvore);
        RascunhoEdital rascunho = RascunhoEdital.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .nomeArquivo("edital-caixa.pdf")
                .conteudoExtraidoJson(json)
                .status(StatusRascunhoEdital.AGUARDANDO_APROVACAO)
                .build();

        when(planoEstudoRepository.save(any(PlanoEstudo.class))).thenAnswer(i -> {
            PlanoEstudo p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(materiaRepository.save(any(Materia.class))).thenAnswer(i -> {
            Materia m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(topicoRepository.save(any(Topico.class))).thenAnswer(i -> {
            Topico t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(rascunhoEditalRepository.save(any(RascunhoEdital.class))).thenAnswer(i -> i.getArgument(0));

        ConverterRascunhoEditalRequest request = new ConverterRascunhoEditalRequest("Meu Plano Caixa 2026", "Foco no edital");

        ConversaoEditalResponse response = support.executarConversao(usuario, rascunho, request);

        assertThat(response).isNotNull();
        assertThat(response.tituloPlano()).isEqualTo("Meu Plano Caixa 2026");
        assertThat(response.totalMateriasCriadas()).isEqualTo(1);
        assertThat(response.totalTopicosCriados()).isEqualTo(3); // 1 pai + 2 subtópicos

        verify(planoEstudoRepository).save(any(PlanoEstudo.class));
        verify(materiaRepository).save(any(Materia.class));
        verify(topicoRepository, times(3)).save(any(Topico.class));
        verify(rascunhoEditalRepository).save(rascunho);
        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunhoEdital.CONVERTIDO);
    }

    @Test
    @DisplayName("Deve normalizar e truncar com segurança titulos longos de tópicos e matérias sem lançar exceção")
    void deveNormalizarTitulosLongosComSeguranca() {
        String tituloLongoTopico = "Topico Extenso ".repeat(100); // > 1000 caracteres
        String subTituloLongo = "Subtopico Extenso ".repeat(80); // > 1000 caracteres
        String nomeLongoMateria = "Materia Extensa ".repeat(30); // > 255 caracteres

        EditalTopicoItem topicoItem = new EditalTopicoItem(tituloLongoTopico, 5, List.of(subTituloLongo));
        EditalMaterialItem materiaItem = new EditalMaterialItem(nomeLongoMateria, List.of(topicoItem));
        EditalArvoreEstruturada arvore = new EditalArvoreEstruturada("Concurso Teste", "Cargo Teste", List.of(materiaItem));

        String json = EditalTreeMapper.toJson(arvore);
        RascunhoEdital rascunho = RascunhoEdital.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .nomeArquivo("edital-longo.pdf")
                .conteudoExtraidoJson(json)
                .status(StatusRascunhoEdital.AGUARDANDO_APROVACAO)
                .build();

        when(planoEstudoRepository.save(any(PlanoEstudo.class))).thenAnswer(i -> {
            PlanoEstudo p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(materiaRepository.save(any(Materia.class))).thenAnswer(i -> {
            Materia m = i.getArgument(0);
            assertThat(m.getNome().length()).isLessThanOrEqualTo(255);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(topicoRepository.save(any(Topico.class))).thenAnswer(i -> {
            Topico t = i.getArgument(0);
            assertThat(t.getTitulo().length()).isLessThanOrEqualTo(1000);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(rascunhoEditalRepository.save(any(RascunhoEdital.class))).thenAnswer(i -> i.getArgument(0));

        ConverterRascunhoEditalRequest request = new ConverterRascunhoEditalRequest("Plano Extenso ".repeat(30), "Descricao");

        ConversaoEditalResponse response = support.executarConversao(usuario, rascunho, request);

        assertThat(response).isNotNull();
        assertThat(response.tituloPlano().length()).isLessThanOrEqualTo(255);
        verify(topicoRepository, times(2)).save(any(Topico.class));
    }
}
