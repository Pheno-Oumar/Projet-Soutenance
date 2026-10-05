package com.kadi_aon.mon_salon.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AjoutPanierDTORequest(
        @NotNull(message = "L'ID du produit est obligatoire.")
        Long produitId,

        @NotNull(message = "La quantité est obligatoire.")
        @Positive(message = "La quantité doit être strictement positive.")
        Integer quantite
) {}
