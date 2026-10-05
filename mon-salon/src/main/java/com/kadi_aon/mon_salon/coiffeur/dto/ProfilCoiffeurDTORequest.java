package com.kadi_aon.mon_salon.coiffeur.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ProfilCoiffeurDTORequest(
        @Size(max = 100, message = "Le nom d'affichage ne peut pas dépasser 100 caractères")
        String nomAffichage,

        String biographie,

        @Min(value = 0, message = "Les années d'expérience ne peuvent pas être négatives")
        Integer anneeExperience,

        String photoProfilUrl,

        @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères")
        String description
) {}
