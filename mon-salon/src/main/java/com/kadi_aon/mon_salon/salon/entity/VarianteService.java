package com.kadi_aon.mon_salon.salon.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variantes_service", uniqueConstraints = {
        @UniqueConstraint(name = "uk_variante_service_nom", columnNames = {"service_salon_id", "nom"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VarianteService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_salon_id", nullable = false)
    private ServiceSalon serviceSalon;

    @Column(nullable = false, length = 150)
    private String nom;

    @Positive(message = "La durée doit être strictement positive")
    @Column(name = "duree_minutes", nullable = false)
    private Integer dureeMinutes;

    @PositiveOrZero(message = "Le prix doit être positif ou nul")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(nullable = false)
    @Builder.Default
    private Boolean statut = true;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;
}
