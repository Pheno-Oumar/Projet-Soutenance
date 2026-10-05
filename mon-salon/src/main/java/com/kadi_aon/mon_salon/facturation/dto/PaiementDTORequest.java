package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaiementDTORequest(
        @NotNull(message = "Le montant du paiement est obligatoire")
        @Positive(message = "Le montant doit être strictement positif")
        BigDecimal montant,

        String moyenPaiement,

        String reference
) {
}
