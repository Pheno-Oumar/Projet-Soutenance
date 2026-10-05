package com.kadi_aon.mon_salon.salon.entity;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "salons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du salon est obligatoire")
    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 255)
    private String logoUrl;

    @NotBlank(message = "Le slug du salon est obligatoire")
    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String adresse;

    @Column(length = 30)
    private String telephone;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "POINT SRID 4326")
    private Point localisation;

    @Column(nullable = false)
    @Builder.Default
    private Boolean statut = true;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = true;
        }
    }

    public Double getLatitude() {
        return (this.localisation != null) ? this.localisation.getY() : null;
    }

    public Double getLongitude() {
        return (this.localisation != null) ? this.localisation.getX() : null;
    }

    public void setCoordonnees(Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            this.localisation = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        } else {
            this.localisation = null;
        }
    }
}
