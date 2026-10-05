package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;

public record ProduitAlerteStockDTOResponse(
        Long produitId,
        String produitNom,
        Long categorieId,
        String categorieNom,
        BigDecimal prixVente,
        Integer quantiteDisponible,
        Integer seuilMinimum,
        Integer seuilMaximum,
        boolean estEnRuptureTotale
) {}
