package com.kadi_aon.mon_salon.salon.dto;

public record SalonCheckDTO(
        boolean exists,
        Long id,
        String nom,
        String slug,
        boolean statut,
        String logoUrl,
        String telephone,
        String adresse
) {}
