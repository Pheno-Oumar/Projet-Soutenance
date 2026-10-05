package com.kadi_aon.mon_salon.reclamation.dto;

import com.kadi_aon.mon_salon.reclamation.enums.StatutReclamation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReclamationTraiterDTORequest(
        @NotNull(message = "Le nouveau statut de traitement est obligatoire.")
        StatutReclamation nouveauStatut,

        @NotBlank(message = "La réponse au client est obligatoire.")
        @Size(max = 2000, message = "La réponse ne doit pas dépasser 2000 caractères.")
        String reponse
) {}
