package com.kadi_aon.mon_salon.avis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvisSalonCreateDTORequest(
        @NotNull(message = "La note est obligatoire.")
        @Min(value = 1, message = "La note minimale est 1.")
        @Max(value = 5, message = "La note maximale est 5.")
        Integer note,

        @Size(max = 1000, message = "Le commentaire ne peut excéder 1000 caractères.")
        String commentaire
) {}
