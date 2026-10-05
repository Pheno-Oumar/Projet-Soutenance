package com.kadi_aon.mon_salon.rendezvous.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coiffeur_affectation_id", nullable = false)
    private AffectationSalon coiffeur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_compte_id", nullable = false)
    private Compte client;

    @Column(name = "date_heure_prevue", nullable = false)
    private LocalDateTime dateHeurePrevue;

    @Column(name = "date_heure_fin", nullable = false)
    private LocalDateTime dateHeureFin;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutRendezVous statut = StatutRendezVous.CONFIRME;

    @Column(name = "montant_estime", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantEstime;

    @OneToMany(mappedBy = "rendezVous", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneRendezVous> lignes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (statut == null) {
            statut = StatutRendezVous.CONFIRME;
        }
    }

    @PreUpdate
    public void preUpdate() {
        dateModification = LocalDateTime.now();
    }

    public void addLigne(LigneRendezVous ligne) {
        lignes.add(ligne);
        ligne.setRendezVous(this);
    }

    /**
     * Machine à états stricte pour RendezVous :
     *   CONFIRME → EN_COURS, ANNULE, NO_SHOW
     *   EN_COURS → TERMINE
     *   TERMINE, ANNULE, NO_SHOW → aucune transition autorisée
     */
    public boolean isTransitionAutorisee(StatutRendezVous nouveauStatut) {
        return switch (this.statut) {
            case CONFIRME -> nouveauStatut == StatutRendezVous.EN_COURS ||
                             nouveauStatut == StatutRendezVous.ANNULE ||
                             nouveauStatut == StatutRendezVous.NO_SHOW;
            case EN_COURS -> nouveauStatut == StatutRendezVous.TERMINE;
            case TERMINE, ANNULE, NO_SHOW -> false;
        };
    }
}
