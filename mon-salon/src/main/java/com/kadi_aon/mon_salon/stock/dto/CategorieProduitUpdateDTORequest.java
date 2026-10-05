package com.kadi_aon.mon_salon.stock.dto;

import jakarta.validation.constraints.NotBlank;

public record CategorieProduitUpdateDTORequest(
        @NotBlank(message = "Le nom de la catégorie est obligatoire.")
        String nom,

        String description
) {}
