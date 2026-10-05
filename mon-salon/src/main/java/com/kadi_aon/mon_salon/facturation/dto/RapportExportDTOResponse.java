package com.kadi_aon.mon_salon.facturation.dto;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;

import lombok.Builder;

@Builder
public record RapportExportDTOResponse(
        Long exportId,
        String slugSalon,
        FormatExportDonnees format,
        String urlTelechargement,
        LocalDateTime dateDemande,
        LocalDateTime dateExpiration,
        String statut
) {}
