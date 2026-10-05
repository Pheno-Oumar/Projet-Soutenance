package com.kadi_aon.mon_salon.coiffeur.entity;

import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profils_coiffeur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilCoiffeur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affectation_id", nullable = false, unique = true)
    private AffectationSalon affectation;

    @Column(name = "nom_affichage", length = 100)
    private String nomAffichage;

    @Column(columnDefinition = "TEXT")
    private String biographie;

    @Column(name = "annee_experience")
    private Integer anneeExperience;

    @Column(name = "photo_profil_url")
    private String photoProfilUrl;

    @Column(length = 255)
    private String description;
}
