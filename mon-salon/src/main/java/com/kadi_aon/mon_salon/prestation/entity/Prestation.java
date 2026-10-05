package com.kadi_aon.mon_salon.prestation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prestations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coiffeur_affectation_id")
    private AffectationSalon coiffeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_compte_id")
    private Compte client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rendez_vous_id", unique = true)
    private RendezVous rendezVous;

    @Column(name = "nom_client", nullable = false, length = 100)
    private String nomClient;

    @Column(name = "prenom_client", nullable = false, length = 100)
    private String prenomClient;

    @Column(name = "telephone_client", length = 30)
    private String telephoneClient;

    @Column(name = "date_heure_debut", nullable = false)
    private LocalDateTime dateHeureDebut;

    @Column(name = "date_heure_fin")
    private LocalDateTime dateHeureFin;

    @Column(name = "montant_total", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutPrestation statut = StatutPrestation.EN_COURS;

    // Numéro d'ordre dans la file d'attente (FIFO pour les walk-in)
    @Column(name = "ordre_file_attente")
    private Integer ordreFileAttente;

    @OneToMany(mappedBy = "prestation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LignePrestation> lignes = new ArrayList<>();

    @OneToOne(mappedBy = "prestation", cascade = CascadeType.ALL)
    private Facture facture;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    public void prePersist() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.dateHeureDebut == null) {
            this.dateHeureDebut = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = StatutPrestation.EN_COURS;
        }
        if (this.montantTotal == null) {
            this.montantTotal = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    /**
     * Vérifie si la transition de statut est autorisée.
     * Machine à états :
     *   EN_ATTENTE → EN_COURS, ANNULEE
     *   EN_COURS   → TERMINEE, ANNULEE
     *   TERMINEE   → (aucune)
     *   ANNULEE    → (aucune)
     */
    public boolean isTransitionAutorisee(StatutPrestation nouveauStatut) {
        return switch (this.statut) {
            case EN_ATTENTE -> nouveauStatut == StatutPrestation.EN_COURS || nouveauStatut == StatutPrestation.ANNULEE;
            case EN_COURS -> nouveauStatut == StatutPrestation.TERMINEE || nouveauStatut == StatutPrestation.ANNULEE;
            case TERMINEE, ANNULEE -> false;
        };
    }
}
