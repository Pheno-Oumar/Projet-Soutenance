package com.kadi_aon.mon_salon.stock.dto;

import jakarta.validation.constraints.NotBlank;

public record RejetCommandeDTORequest(
        @NotBlank(message = "Le motif du rejet est obligatoire.")
        String motif
) {}
