package com.kadi_aon.mon_salon.rgpd.dto;

import java.time.LocalDateTime;

public record DemandeExportDTOResponse(
        Long id,
        String compteEmail,
        String format,
        String statut,
        String urlTelechargement,
        LocalDateTime dateDemande,
        LocalDateTime dateExpiration,
        boolean expire
) {}
