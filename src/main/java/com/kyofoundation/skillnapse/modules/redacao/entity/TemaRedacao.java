package com.kyofoundation.skillnapse.modules.redacao.entity;

import com.kyofoundation.skillnapse.modules.planoestudo.entity.PlanoEstudo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "temas_redacao")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "titulo", "geradoPorIa"})
@EqualsAndHashCode(of = "id")
public class TemaRedacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_estudo_id")
    private PlanoEstudo planoEstudo;

    @Column(name = "titulo", length = 200, nullable = false)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "textos_motivadores", columnDefinition = "TEXT", nullable = false)
    private String textosMotivadores;

    @Column(name = "criterios_avaliacao", columnDefinition = "TEXT")
    private String criteriosAvaliacao;

    @Builder.Default
    @Column(name = "gerado_por_ia", nullable = false)
    private Boolean geradoPorIa = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Builder.Default
    @OneToMany(mappedBy = "temaRedacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissaoRedacao> submissoes = new ArrayList<>();
}
