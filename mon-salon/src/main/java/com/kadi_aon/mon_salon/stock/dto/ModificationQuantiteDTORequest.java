package com.kadi_aon.mon_salon.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ModificationQuantiteDTORequest(
        @NotNull(message = "La quantité est obligatoire.")
        @Positive(message = "La quantité doit être strictement positive.")
        Integer quantite
) {}
