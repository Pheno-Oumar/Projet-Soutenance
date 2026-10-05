package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;

public record LignePanierDTOResponse(
        Long id,
        Long produitId,
        String produitNom,
        BigDecimal prixUnitaire,
        Integer quantite,
        BigDecimal sousTotal,
        Integer stockDisponible,
        boolean enAlerte
) {}
