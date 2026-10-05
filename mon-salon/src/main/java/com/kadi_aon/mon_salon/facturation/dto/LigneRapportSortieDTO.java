package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;

import lombok.Builder;

@Builder
public record LigneRapportSortieDTO(
        Long depenseId,
        BigDecimal montant,
        CategorieDepense categorie,
        LocalDateTime dateDepense,
        String description,
        String comptableNom,
        boolean statut
) {}
