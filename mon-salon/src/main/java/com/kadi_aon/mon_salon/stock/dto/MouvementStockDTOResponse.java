package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MouvementStockDTOResponse(
        Long id,
        Long produitId,
        String produitNom,
        Integer quantite,
        String type,
        BigDecimal prixUnitaire,
        LocalDateTime dateMouvement,
        String motif,
        String auteurNomComplet,
        Integer quantiteRestante
) {}
