package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaiementDTOResponse(
        Long id,
        String numeroPaiement,
        BigDecimal montant,
        LocalDateTime datePaiement,
        String type,
        String statut,
        Long factureId,
        String numeroFacture,
        Long prestationId,
        String salonSlug,
        String nomClient,
        String prenomClient,
        String moyenPaiement,
        String reference,
        // Nouveaux champs pour la traçabilité remboursement
        Long paiementOrigineId,
        String paiementOrigineNumero,
        BigDecimal montantRembourse,
        BigDecimal montantRemboursable
) {
    // Constructeur rétro-compatible pour les tests et usages simples
    public PaiementDTOResponse(
            Long id,
            String numeroPaiement,
            BigDecimal montant,
            LocalDateTime datePaiement,
            String type,
            String statut,
            Long factureId,
            String numeroFacture,
            Long prestationId,
            String salonSlug,
            String nomClient,
            String prenomClient
    ) {
        this(id, numeroPaiement, montant, datePaiement, type, statut, factureId, numeroFacture, prestationId, salonSlug, nomClient, prenomClient, null, null, null, null, BigDecimal.ZERO, montant);
    }
}
