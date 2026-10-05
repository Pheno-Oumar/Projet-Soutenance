package com.kadi_aon.mon_salon.depense.dto;

import java.math.BigDecimal;

import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepenseCreateDTORequest(
        @NotNull(message = "Le montant de la dépense est obligatoire.")
        @Positive(message = "Le montant doit être strictement positif.")
        BigDecimal montant,

        @NotNull(message = "La catégorie de dépense est obligatoire.")
        CategorieDepense categorie,

        String description
) {}
