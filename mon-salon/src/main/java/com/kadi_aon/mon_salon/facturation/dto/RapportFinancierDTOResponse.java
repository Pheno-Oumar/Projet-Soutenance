package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;
import com.kadi_aon.mon_salon.facturation.enums.TypeRapportFinancier;

import lombok.Builder;

@Builder
public record RapportFinancierDTOResponse(
        String slugSalon,
        LocalDate dateDebut,
        LocalDate dateFin,
        TypeRapportFinancier typeRapport,
        CategorieDepense categorieDepense,
        BigDecimal totalEntrees,
        BigDecimal totalSorties,
        BigDecimal soldeNet,
        int nombreEntrees,
        int nombreSorties,
        List<LigneRapportEntreeDTO> entrees,
        List<LigneRapportSortieDTO> sorties
) {}
