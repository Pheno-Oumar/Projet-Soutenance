package com.kadi_aon.mon_salon.salon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalonDTOResponse(
        Long id,
        String nom,
        String slug,
        String logoUrl,
        String description,
        String adresse,
        String telephone,
        String email,
        Double latitude,
        Double longitude,
        Boolean statut,
        LocalDateTime dateCreation,
        String emailProprietaire,
        Long nombreClients,
        Long nombreEmployes,
        BigDecimal chiffreAffaires,
        Long nombreCommandes
) {
    public SalonDTOResponse(
            Long id,
            String nom,
            String slug,
            String logoUrl,
            String description,
            String adresse,
            String telephone,
            String email,
            Double latitude,
            Double longitude,
            Boolean statut,
            LocalDateTime dateCreation,
            String emailProprietaire
    ) {
        this(id, nom, slug, logoUrl, description, adresse, telephone, email, latitude, longitude,
                statut, dateCreation, emailProprietaire, 0L, 0L, BigDecimal.ZERO, 0L);
    }
}
