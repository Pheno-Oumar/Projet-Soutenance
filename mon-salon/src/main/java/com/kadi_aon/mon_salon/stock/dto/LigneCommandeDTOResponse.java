package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;

public record LigneCommandeDTOResponse(
        Long id,
        Long produitId,
        String produitNom,
        Integer quantite,
        BigDecimal prixUnitaire,
        BigDecimal sousTotal
) {}
