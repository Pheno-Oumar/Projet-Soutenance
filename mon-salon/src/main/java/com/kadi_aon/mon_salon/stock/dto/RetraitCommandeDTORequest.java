package com.kadi_aon.mon_salon.stock.dto;

import jakarta.validation.constraints.NotBlank;

public record RetraitCommandeDTORequest(
        @NotBlank(message = "Le code de retrait est obligatoire.")
        String codeRetrait
) {}
