package com.kadi_aon.mon_salon.realisation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentaireCreateDTORequest(
        @NotBlank(message = "Le contenu du commentaire ne peut pas être vide.")
        @Size(max = 1000, message = "Le commentaire ne peut pas dépasser 1000 caractères.")
        String contenu
) {}
