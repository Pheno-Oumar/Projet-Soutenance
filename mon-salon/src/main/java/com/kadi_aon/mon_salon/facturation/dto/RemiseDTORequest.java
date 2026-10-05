package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RemiseDTORequest(
        @NotNull(message = "Le montant de la remise est obligatoire")
        @Positive(message = "Le montant de la remise doit être strictement positif")
        BigDecimal montant,

        @NotBlank(message = "Le motif de la remise est obligatoire")
        String motif
) {
}
