package com.kadi_aon.mon_salon.regle.dto;

import jakarta.validation.constraints.NotBlank;

public record RegleSalonCreateDTORequest(
        @NotBlank(message = "Le titre de la règle est obligatoire")
        String titre,

        @NotBlank(message = "La description de la règle est obligatoire")
        String description
) {}
