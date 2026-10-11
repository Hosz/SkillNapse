package com.kyofoundation.skillnapse.modules.cronograma.entity;

import com.kyofoundation.skillnapse.modules.auth.entity.Usuario;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoAcaoExcecao;
import com.kyofoundation.skillnapse.modules.cronograma.enums.TipoBloco;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Materia;
import com.kyofoundation.skillnapse.modules.planoestudo.entity.Topico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "excecoes_diarias")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "dataExcecao", "tipoAcao", "horaInicio", "horaFim"})
@EqualsAndHashCode(of = "id")
public class ExcecaoDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_excecao", nullable = false)
    private LocalDate dataExcecao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloco_template_origem_id")
    private BlocoHorarioTemplate blocoTemplateOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acao", length = 25, nullable = false)
    private TipoAcaoExcecao tipoAcao;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_bloco", length = 20)
    private TipoBloco tipoBloco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topico_id")
    private Topico topico;

    @Column(name = "titulo", length = 255)
    private String titulo;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "excecoes_topicos_revisao",
            joinColumns = @JoinColumn(name = "excecao_id"),
            inverseJoinColumns = @JoinColumn(name = "topico_id")
    )
    private Set<Topico> topicosRevisao = new HashSet<>();
}
