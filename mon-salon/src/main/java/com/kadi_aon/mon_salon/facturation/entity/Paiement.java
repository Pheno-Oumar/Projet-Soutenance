package com.kadi_aon.mon_salon.facturation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.facturation.enums.MoyenPaiement;
import com.kadi_aon.mon_salon.facturation.enums.StatutPaiement;
import com.kadi_aon.mon_salon.facturation.enums.TypePaiement;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "paiements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_paiement", nullable = false, unique = true, length = 50)
    private String numeroPaiement;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypePaiement type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutPaiement statut = StatutPaiement.PAYE;

    @Enumerated(EnumType.STRING)
    @Column(name = "moyen_paiement", length = 30)
    @Builder.Default
    private MoyenPaiement moyenPaiement = MoyenPaiement.ESPECES;

    @Column(name = "reference", length = 100)
    private String reference;

    // --- FK vers le paiement d'origine (pour les remboursements) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiement_origine_id")
    private Paiement paiementOrigine;

    // Montant déjà remboursé sur CE paiement (mis à jour à chaque remboursement)
    @Column(name = "montant_rembourse", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantRembourse = BigDecimal.ZERO;

    // Remboursements liés à ce paiement (enfants)
    @OneToMany(mappedBy = "paiementOrigine", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Paiement> remboursements = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encaisse_par_affectation_id")
    private AffectationSalon encaissePar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_compte_id")
    private Compte client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_caisse_id")
    private OperationCaisse operationCaisse;

    @PrePersist
    public void prePersist() {
        if (this.datePaiement == null) {
            this.datePaiement = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = StatutPaiement.PAYE;
        }
        if (this.montantRembourse == null) {
            this.montantRembourse = BigDecimal.ZERO;
        }
    }

    /**
     * Montant encore remboursable sur ce paiement.
     */
    public BigDecimal getMontantRemboursable() {
        BigDecimal rem = this.montantRembourse != null ? this.montantRembourse : BigDecimal.ZERO;
        BigDecimal restant = this.montant.subtract(rem);
        return restant.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : restant;
    }

    /**
     * Vérifie si ce paiement peut encore être remboursé.
     */
    public boolean isRemboursable() {
        return (this.statut == StatutPaiement.PAYE || this.statut == StatutPaiement.PARTIELLEMENT_REMBOURSE)
                && getMontantRemboursable().compareTo(BigDecimal.ZERO) > 0;
    }
}
