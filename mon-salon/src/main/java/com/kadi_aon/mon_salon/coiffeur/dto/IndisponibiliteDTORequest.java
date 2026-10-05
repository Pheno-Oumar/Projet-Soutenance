package com.kadi_aon.mon_salon.coiffeur.dto;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.coiffeur.enums.MotifIndisponibilite;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IndisponibiliteDTORequest(
        Long coiffeurAffectationId,

        @NotNull(message = "La date de début est obligatoire")
        LocalDateTime dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDateTime dateFin,

        @NotNull(message = "Le motif d'indisponibilité est obligatoire")
        MotifIndisponibilite motif,

        @Size(max = 255, message = "Le commentaire ne peut pas dépasser 255 caractères")
        String commentaire
) {
    public IndisponibiliteDTORequest(
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            MotifIndisponibilite motif,
            String commentaire
    ) {
        this(null, dateDebut, dateFin, motif, commentaire);
    }
}

