package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Builder;

@Builder
public record KpiFinancierDTOResponse(
        String slugSalon,
        BigDecimal chiffreAffairesTotal,
        BigDecimal revenusMoisEnCours,
        BigDecimal depensesMoisEnCours,
        BigDecimal beneficeNetMoisEnCours,
        BigDecimal revenusAujourdhui,
        BigDecimal depensesAujourdhui,
        BigDecimal beneficeNetAujourdhui,
        BigDecimal totalRemboursements,
        Map<String, BigDecimal> depensesParCategorie,
        boolean sessionCaisseOuverte,
        BigDecimal soldeTheoriqueCaisseActive
) {}
