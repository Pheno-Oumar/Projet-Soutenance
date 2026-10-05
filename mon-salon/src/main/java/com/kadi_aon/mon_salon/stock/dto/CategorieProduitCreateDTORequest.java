package com.kadi_aon.mon_salon.stock.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CategorieProduitCreateDTORequest(
        @NotBlank(message = "Le nom de la catégorie est obligatoire.")
        String nom,

        String description,

        List<@Valid ProduitInitialDTORequest> produits
) {}
