package com.kadi_aon.mon_salon.avis.dto;

import jakarta.validation.constraints.NotNull;

public record AvisModerationDTORequest(
        @NotNull(message = "Le statut est obligatoire.")
        Boolean statut
) {}
