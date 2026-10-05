package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RemboursementDTORequest(
        @NotNull(message = "Le montant du remboursement est obligatoire")
        @Positive(message = "Le montant doit être strictement positif")
        BigDecimal montant,

        @NotBlank(message = "Le motif du remboursement est obligatoire")
        String motif,

        Boolean annulerPrestation
) {
    public RemboursementDTORequest(BigDecimal montant, String motif) {
        this(montant, motif, false);
    }
}
