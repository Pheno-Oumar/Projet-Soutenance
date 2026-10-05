package com.kadi_aon.mon_salon.caisse.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SessionCaisseDTOResponse(
        Long id,
        Long affectationId,
        String comptableNom,
        String comptablePrenom,
        String comptableEmail,
        LocalDateTime dateOuverture,
        LocalDateTime dateCloture,
        BigDecimal soldeOuverture,
        BigDecimal soldeFermeture,
        BigDecimal totalEntrees,
        BigDecimal totalSorties,
        BigDecimal soldeTheorique,
        String statut,
        List<OperationCaisseDTOResponse> operations
) {}
