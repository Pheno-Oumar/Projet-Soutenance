package com.kadi_aon.mon_salon.salon.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record VariantePrixUpdateDTORequest(
        @NotNull(message = "Le nouveau prix est obligatoire")
        @PositiveOrZero(message = "Le prix doit être positif ou nul")
        BigDecimal prix
) {}
