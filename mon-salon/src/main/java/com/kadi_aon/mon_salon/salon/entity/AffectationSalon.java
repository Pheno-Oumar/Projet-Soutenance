package com.kadi_aon.mon_salon.salon.entity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.kadi_aon.mon_salon.account.entity.Compte;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "affectations_salon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationSalon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "affectation_roles",
            joinColumns = @JoinColumn(name = "affectation_id"),
            inverseJoinColumns = @JoinColumn(name = "role_salon_id")
    )
    @Builder.Default
    private Set<RoleSalon> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean statut = true;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @PrePersist
    public void prePersist() {
        if (this.dateDebut == null) {
            this.dateDebut = LocalDate.now();
        }
        if (this.statut == null) {
            this.statut = true;
        }
    }
}
