package com.kadi_aon.mon_salon.realisation.dto;

import jakarta.validation.constraints.NotNull;

public record RealisationPublicationDTORequest(
        @NotNull(message = "Le statut de publication est obligatoire.")
        Boolean statutPublication
) {}
