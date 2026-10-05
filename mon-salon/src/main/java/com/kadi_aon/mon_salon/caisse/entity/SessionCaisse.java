package com.kadi_aon.mon_salon.caisse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sessions_caisse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCaisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affectation_id", nullable = false)
    private AffectationSalon affectation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDateTime dateOuverture;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "solde_ouverture", nullable = false, precision = 12, scale = 2)
    private BigDecimal soldeOuverture;

    @Column(name = "solde_fermeture", precision = 12, scale = 2)
    private BigDecimal soldeFermeture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutSessionCaisse statut = StatutSessionCaisse.EN_COURS;

    @OneToMany(mappedBy = "sessionCaisse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OperationCaisse> operations = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.dateOuverture == null) {
            this.dateOuverture = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = StatutSessionCaisse.EN_COURS;
        }
        if (this.soldeOuverture == null) {
            this.soldeOuverture = BigDecimal.ZERO;
        }
        if (this.salon == null && this.affectation != null && this.affectation.getSalon() != null) {
            this.salon = this.affectation.getSalon();
        }
    }
}
