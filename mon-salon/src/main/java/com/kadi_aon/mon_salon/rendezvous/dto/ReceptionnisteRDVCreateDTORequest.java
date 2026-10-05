package com.kadi_aon.mon_salon.rendezvous.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ReceptionnisteRDVCreateDTORequest(
        @NotNull(message = "L'email ou identifiant du client est obligatoire")
        String clientEmail,

        @NotNull(message = "La date et l'heure prévues sont obligatoires")
        LocalDateTime dateHeurePrevue,

        @NotEmpty(message = "Vous devez sélectionner au moins une variante de service")
        List<Long> varianteIds,

        Long coiffeurId
) {}
