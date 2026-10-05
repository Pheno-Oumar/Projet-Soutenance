package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;

public record KpiStockDTOResponse(
        long totalProduitsActifs,
        long totalProduitsEnAlerte,
        long totalProduitsEnRupture,
        BigDecimal valeurTotaleStock,
        long totalCommandes,
        long commandesEnAttente,
        long commandesValidees,
        long commandesRejetees,
        long commandesRecuperees,
        BigDecimal chiffreAffairesVentes
) {}
