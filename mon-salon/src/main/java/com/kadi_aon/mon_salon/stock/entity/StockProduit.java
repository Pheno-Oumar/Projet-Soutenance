package com.kadi_aon.mon_salon.stock.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_produit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produit_id", nullable = false, unique = true)
    private Produit produit;

    @Builder.Default
    @PositiveOrZero(message = "La quantité disponible ne peut être négative.")
    @Column(name = "quantite_disponible", nullable = false)
    private Integer quantiteDisponible = 0;

    @Builder.Default
    @PositiveOrZero(message = "Le seuil minimum ne peut être négatif.")
    @Column(name = "seuil_minimum", nullable = false)
    private Integer seuilMinimum = 0;

    @Column(name = "seuil_maximum")
    private Integer seuilMaximum;

    @Column(name = "date_derniere_mise_a_jour", nullable = false)
    private LocalDateTime dateDerniereMiseAJour;
}
