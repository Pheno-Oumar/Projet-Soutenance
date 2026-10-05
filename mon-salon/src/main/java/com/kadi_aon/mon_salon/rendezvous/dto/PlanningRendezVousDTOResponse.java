package com.kadi_aon.mon_salon.rendezvous.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlanningRendezVousDTOResponse(
        Long rdvId,
        Long coiffeurAffectationId,
        String coiffeurNom,
        String coiffeurPrenom,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        String nomClient,
        String prenomClient,
        String telephoneClient,
        Long clientCompteId,
        String statut,
        BigDecimal montantEstime,
        List<LigneRendezVousDTOResponse> prestations
) {
}
