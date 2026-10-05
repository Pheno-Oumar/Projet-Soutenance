package com.kadi_aon.mon_salon.salon.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SalonCreateDTORequest(
        @NotBlank(message = "Le nom du salon est obligatoire")
        String nom,

        @NotBlank(message = "L'email du propriétaire est obligatoire")
        @Email(message = "Format d'email du propriétaire invalide")
        String emailProprietaire,

        String description,
        String adresse,
        String telephone,
        String email,
        Double latitude,
        Double longitude,
        String logoUrl
) {
    public SalonCreateDTORequest(String nom, String emailProprietaire) {
        this(nom, emailProprietaire, null, null, null, null, null, null, null);
    }
}
