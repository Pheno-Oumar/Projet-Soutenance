package com.kadi_aon.mon_salon.facturation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FactureDTOResponse(
                Long id,
                String numeroFacture,
                LocalDateTime dateEmission,
                BigDecimal montantTotal,
                BigDecimal remise,
                String motifRemise,
                BigDecimal montantNet,
                BigDecimal montantPaye,
                BigDecimal resteAPayer,
                Long prestationId,
                Long commandeId,
                List<PaiementDTOResponse> paiements,
                String statut,
                String clientNom,
                String clientTelephone,
                String coiffeurNom,
                List<com.kadi_aon.mon_salon.prestation.dto.LignePrestationDTOResponse> lignesPrestation) {

        // Constructeur rétro-compatible à 14 paramètres
        public FactureDTOResponse(
                        Long id,
                        String numeroFacture,
                        LocalDateTime dateEmission,
                        BigDecimal montantTotal,
                        BigDecimal remise,
                        String motifRemise,
                        BigDecimal montantNet,
                        BigDecimal montantPaye,
                        BigDecimal resteAPayer,
                        Long prestationId,
                        Long commandeId,
                        List<PaiementDTOResponse> paiements,
                        String statut,
                        String clientNom) {
                this(id, numeroFacture, dateEmission, montantTotal, remise, motifRemise, montantNet, montantPaye,
                                resteAPayer, prestationId, commandeId, paiements, statut, clientNom, null, null, null);
        }

        // Constructeur rétro-compatible pour les tests et usages simples (8 paramètres)
        public FactureDTOResponse(
                        Long id,
                        String numeroFacture,
                        LocalDateTime dateEmission,
                        BigDecimal montantTotal,
                        BigDecimal montantPaye,
                        BigDecimal resteAPayer,
                        Long prestationId,
                        List<PaiementDTOResponse> paiements) {
                this(id, numeroFacture, dateEmission, montantTotal, BigDecimal.ZERO, null, montantTotal, montantPaye,
                                resteAPayer, prestationId, null, paiements, null, null, null, null, null);
        }
}
