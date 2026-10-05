package com.kadi_aon.mon_salon.rgpd.dto;

import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;

import jakarta.validation.constraints.NotNull;

public record DemandeExportDTORequest(
        @NotNull(message = "Le format d'export (JSON ou CSV) est obligatoire.")
        FormatExportDonnees format
) {}
