package com.kadi_aon.mon_salon.caisse.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SessionCaisseClotureDTORequest(
        @NotNull(message = "Le solde de fermeture est obligatoire")
        @PositiveOrZero(message = "Le solde de fermeture doit être positif ou nul")
        BigDecimal soldeFermeture
) {}
