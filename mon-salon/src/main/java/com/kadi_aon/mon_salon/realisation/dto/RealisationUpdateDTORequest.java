package com.kadi_aon.mon_salon.realisation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RealisationUpdateDTORequest(
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères.")
        String titre,

        @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères.")
        String description,

        Long coiffeurId,

        LocalDate dateRealisation
) {}
