package com.kadi_aon.mon_salon.rgpd.entity;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;
import com.kadi_aon.mon_salon.rgpd.enums.StatutExportDonnees;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "demande_export_donnees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeExportDonnees {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormatExportDonnees format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutExportDonnees statut = StatutExportDonnees.EN_ATTENTE;

    @Column(name = "url_telechargement", length = 1000)
    private String urlTelechargement;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    @PrePersist
    public void prePersist() {
        if (this.dateDemande == null) {
            this.dateDemande = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = StatutExportDonnees.EN_ATTENTE;
        }
    }
}
