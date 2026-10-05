package com.kadi_aon.mon_salon.stock.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record VitrineCommandeDTORequest(
        @NotBlank(message = "Le nom du client est obligatoire")
        String nom,

        String prenom,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        String telephone,

        String email,

        String password,

        @NotEmpty(message = "Le panier ne peut pas être vide")
        List<LigneVitrineCommandeDTORequest> lignes
) {
    public record LigneVitrineCommandeDTORequest(
            Long produitId,
            int quantite
    ) {}
}
