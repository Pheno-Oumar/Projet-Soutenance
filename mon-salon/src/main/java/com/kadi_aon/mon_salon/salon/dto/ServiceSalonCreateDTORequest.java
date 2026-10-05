package com.kadi_aon.mon_salon.salon.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ServiceSalonCreateDTORequest(
        @NotBlank(message = "Le nom du service est obligatoire")
        String nom,

        String description,

        @Valid
        List<VarianteCreateDTORequest> variantes
) {}
