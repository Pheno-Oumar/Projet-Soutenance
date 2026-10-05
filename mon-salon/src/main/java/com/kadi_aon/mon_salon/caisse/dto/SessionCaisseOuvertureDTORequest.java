package com.kadi_aon.mon_salon.caisse.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SessionCaisseOuvertureDTORequest(
        @NotNull(message = "Le solde d'ouverture est obligatoire")
        @PositiveOrZero(message = "Le solde d'ouverture doit être positif ou nul")
        BigDecimal soldeOuverture
) {}
