package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProduitUpdateDTORequest(
        @NotBlank(message = "Le nom du produit est obligatoire.")
        String nom,

        String description,

        @NotNull(message = "Le prix de vente est obligatoire.")
        @PositiveOrZero(message = "Le prix de vente doit être positif ou nul.")
        BigDecimal prixVente,

        @PositiveOrZero(message = "Le seuil minimum doit être positif ou nul.")
        Integer seuilMinimum,

        @PositiveOrZero(message = "Le seuil maximum doit être positif ou nul.")
        Integer seuilMaximum
) {}
