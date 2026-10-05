package com.kadi_aon.mon_salon.prestation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LignePrestationDTORequest(
        @NotNull(message = "L'ID de la variante de service est obligatoire")
        Long varianteServiceId,

        @NotNull(message = "Le prix réel est obligatoire")
        @Positive(message = "Le prix réel doit être strictement positif")
        BigDecimal prixReel
) {}
