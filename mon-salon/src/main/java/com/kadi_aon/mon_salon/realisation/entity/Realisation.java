package com.kadi_aon.mon_salon.realisation.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.salon.entity.Salon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "realisation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Realisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre de la réalisation est obligatoire.")
    @Column(nullable = false)
    private String titre;

    @Column(length = 1000)
    private String description;

    @Column(name = "url_video")
    private String urlVideo; // Stocké en base, hébergé sur Cloudinary

    @Column(name = "date_realisation")
    private LocalDate dateRealisation;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @Builder.Default
    @Column(name = "statut_publication", nullable = false)
    private boolean statutPublication = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Builder.Default
    @Column(name = "total_likes", nullable = false)
    private int totalLikes = 0;

    @Builder.Default
    @Column(name = "total_commentaires", nullable = false)
    private int totalCommentaires = 0;

    @Builder.Default
    @Column(name = "total_vues", nullable = false)
    private long totalVues = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coiffeur_id")
    private Compte coiffeur;
}
