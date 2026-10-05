package com.kadi_aon.mon_salon.rgpd.dto;

import jakarta.validation.constraints.Size;

public record DemandeSuppressionDTORequest(
        @Size(max = 1000, message = "Le motif ne doit pas dépasser 1000 caractères.")
        String motif
) {}
