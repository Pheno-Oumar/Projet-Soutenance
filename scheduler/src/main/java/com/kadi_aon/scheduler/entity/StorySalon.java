package com.kadi_aon.scheduler.entity;

import java.time.LocalDateTime;

import com.kadi_aon.scheduler.enums.TypeMediaStory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stories_salon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorySalon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "salon_id", nullable = false)
    private Long salonId;

    @Column(name = "auteur_id", nullable = false)
    private Long auteurId;

    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_media", nullable = false, length = 20)
    private TypeMediaStory typeMedia;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;
}
