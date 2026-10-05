package com.kadi_aon.mon_salon.prestation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kadi_aon.mon_salon.facturation.dto.FactureDTOResponse;

public record PrestationDTOResponse(
        Long id,
        String salonSlug,
        Long coiffeurAffectationId,
        String coiffeurNom,
        String coiffeurPrenom,
        Long clientCompteId,
        String nomClient,
        String prenomClient,
        String telephoneClient,
        Long rendezVousId,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        BigDecimal montantTotal,
        String statut,
        FactureDTOResponse facture,
        List<LignePrestationDTOResponse> lignes
) {}
