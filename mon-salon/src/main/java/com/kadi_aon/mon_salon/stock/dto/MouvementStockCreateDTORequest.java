package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;

import com.kadi_aon.mon_salon.stock.enums.TypeMouvementStock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record MouvementStockCreateDTORequest(
        @NotNull(message = "L'identifiant du produit est obligatoire.")
        Long produitId,

        @NotNull(message = "La quantité est obligatoire.")
        @Positive(message = "La quantité doit être strictement positive.")
        Integer quantite,

        @NotNull(message = "Le type de mouvement est obligatoire.")
        TypeMouvementStock type,

        @PositiveOrZero(message = "Le prix unitaire doit être positif ou nul.")
        BigDecimal prixUnitaire,

        String motif
) {}
