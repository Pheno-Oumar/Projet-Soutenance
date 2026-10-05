package com.kadi_aon.mon_salon.rgpd.dto;

import java.time.LocalDateTime;

public record DemandeSuppressionDTOResponse(
        Long id,
        Long compteId,
        String compteEmail,
        String motif,
        String statut,
        LocalDateTime dateDemande,
        LocalDateTime dateDecision,
        String motifDecision,
        String traiteParAdminEmail
) {}
