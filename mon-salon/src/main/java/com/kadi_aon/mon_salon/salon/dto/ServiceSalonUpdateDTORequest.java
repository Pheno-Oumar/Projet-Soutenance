package com.kadi_aon.mon_salon.salon.dto;

import jakarta.validation.constraints.NotBlank;

public record ServiceSalonUpdateDTORequest(
        @NotBlank(message = "Le nom du service est obligatoire")
        String nom,

        String description
) {}
