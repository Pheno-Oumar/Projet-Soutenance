package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CommandeDTOResponse(
        Long id,
        String numeroCommande,
        LocalDateTime dateCommande,
        String statut,
        BigDecimal montantTotal,
        String clientNom,
        String clientEmail,
        String codeRetrait,
        String motifRejet,
        LocalDateTime dateTraitement,
        String traiteParNom,
        Long factureId,
        String numeroFacture,
        List<LigneCommandeDTOResponse> lignes
) {}
