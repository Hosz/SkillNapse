package com.kyofoundation.skillnapse.modules.planoestudo.entity;

import com.kyofoundation.skillnapse.modules.planoestudo.enums.NivelProficiencia;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "topicos")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "titulo", "nivelProficiencia", "pesoEdital", "concluido", "ordem"})
@EqualsAndHashCode(of = "id")
public class Topico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topico_pai_id")
    private Topico topicoPai;

    @Column(name = "titulo", length = 1000, nullable = false)
    private String titulo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_proficiencia", length = 20, nullable = false)
    private NivelProficiencia nivelProficiencia = NivelProficiencia.INICIANTE;

    @Builder.Default
    @Column(name = "peso_edital", nullable = false)
    private Integer pesoEdital = 1;

    @Builder.Default
    @Column(name = "concluido", nullable = false)
    private Boolean concluido = false;

    @Builder.Default
    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Builder.Default
    @OrderBy("ordem ASC, criadoEm ASC")
    @OneToMany(mappedBy = "topicoPai", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Topico> subtopicos = new ArrayList<>();
}
