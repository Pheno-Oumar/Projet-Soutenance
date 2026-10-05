package com.kadi_aon.mon_salon.rgpd.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DemandeSuppressionDecisionDTORequest(
        @NotNull(message = "La décision (approuvée ou rejetée) est obligatoire.")
        Boolean approuvee,

        @Size(max = 1000, message = "Le motif de la décision ne doit pas dépasser 1000 caractères.")
        String motifDecision
) {}
