package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.facturation.enums.TypePaiement;

import lombok.Builder;

@Builder
public record LigneRapportEntreeDTO(
        Long paiementId,
        String numeroPaiement,
        String numeroFacture,
        BigDecimal montant,
        TypePaiement modePaiement,
        LocalDateTime datePaiement,
        String clientNom,
        String statut
) {}
