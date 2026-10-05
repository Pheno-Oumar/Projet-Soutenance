package com.kadi_aon.mon_salon.profilcapillaire.entity;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.account.entity.Compte;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "profils_capillaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilCapillaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false, unique = true)
    private Compte compte;

    @Column(name = "type_cheveux", length = 50)
    private String typeCheveux;

    @Column(length = 50)
    private String texture;

    @Column(length = 50)
    private String longueur;

    @Column(length = 50)
    private String densite;

    @Column(name = "cuir_chevelu", length = 50)
    private String cuirChevelu;

    @Column(name = "etat_cheveux", length = 100)
    private String etatCheveux;

    @Column(columnDefinition = "TEXT")
    private String sensibilites;

    @Column(name = "allergies_produits", columnDefinition = "TEXT")
    private String allergiesProduits;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "code_profil", length = 255)
    private String codeProfil;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
        this.dateMiseAJour = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.dateMiseAJour = LocalDateTime.now();
    }
}
