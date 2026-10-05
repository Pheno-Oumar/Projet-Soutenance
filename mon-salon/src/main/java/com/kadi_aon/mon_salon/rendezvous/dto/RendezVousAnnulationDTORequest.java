package com.kadi_aon.mon_salon.rendezvous.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RendezVousAnnulationDTORequest(
        @NotBlank(message = "Le motif d'annulation est obligatoire")
        @Size(max = 255, message = "Le motif ne doit pas dépasser 255 caractères")
        String motif
) {
}
