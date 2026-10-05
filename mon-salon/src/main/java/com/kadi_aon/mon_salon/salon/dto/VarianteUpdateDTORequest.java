package com.kadi_aon.mon_salon.salon.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record VarianteUpdateDTORequest(
        @NotBlank(message = "Le nom de la variante est obligatoire")
        String nom,

        @NotNull(message = "La durée en minutes est obligatoire")
        @Positive(message = "La durée doit être strictement supérieure à zéro")
        Integer dureeMinutes,

        @NotNull(message = "Le prix est obligatoire")
        @PositiveOrZero(message = "Le prix doit être positif ou nul")
        BigDecimal prix
) {}
