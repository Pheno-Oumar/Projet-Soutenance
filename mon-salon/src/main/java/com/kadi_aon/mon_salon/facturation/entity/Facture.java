package com.kadi_aon.mon_salon.facturation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kadi_aon.mon_salon.facturation.enums.StatutFacture;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.stock.entity.Commande;

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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "factures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_facture", nullable = false, unique = true, length = 50)
    private String numeroFacture;

    @Column(name = "date_emission", nullable = false)
    private LocalDateTime dateEmission;

    @Column(name = "montant_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTotal;

    @Column(name = "montant_paye", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "reste_a_payer", nullable = false, precision = 12, scale = 2)
    private BigDecimal resteAPayer;

    // Remise commerciale / geste
    @Column(name = "remise", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal remise = BigDecimal.ZERO;

    @Column(name = "motif_remise", length = 255)
    private String motifRemise;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutFacture statut = StatutFacture.EMISE;

    // Verrouillage optimiste contre les doubles encaissements concurrentiels
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestation_id", unique = true)
    private Prestation prestation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", unique = true)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_affectation_id")
    private AffectationSalon clientAffectation;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Paiement> paiements = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.dateEmission == null) {
            this.dateEmission = LocalDateTime.now();
        }
        if (this.montantTotal == null) {
            this.montantTotal = BigDecimal.ZERO;
        }
        if (this.montantPaye == null) {
            this.montantPaye = BigDecimal.ZERO;
        }
        if (this.remise == null) {
            this.remise = BigDecimal.ZERO;
        }
        if (this.resteAPayer == null) {
            this.resteAPayer = getMontantNet();
        }
        if (this.statut == null) {
            this.statut = StatutFacture.EMISE;
        }
        if (this.salon == null) {
            if (this.prestation != null && this.prestation.getSalon() != null) {
                this.salon = this.prestation.getSalon();
            } else if (this.clientAffectation != null && this.clientAffectation.getSalon() != null) {
                this.salon = this.clientAffectation.getSalon();
            }
        }
    }

    /**
     * Montant net après remise. C'est le montant réellement dû par le client.
     */
    public BigDecimal getMontantNet() {
        BigDecimal total = this.montantTotal != null ? this.montantTotal : BigDecimal.ZERO;
        BigDecimal rem = this.remise != null ? this.remise : BigDecimal.ZERO;
        BigDecimal net = total.subtract(rem);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }
}
