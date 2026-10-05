package com.kadi_aon.mon_salon.rendezvous.dto;

import java.time.LocalDateTime;

public record RetardRendezVousDTOResponse(
        Long rdvId,
        String nomClient,
        String prenomClient,
        String telephoneClient,
        LocalDateTime dateHeurePrevue,
        LocalDateTime dateHeureFin,
        long minutesDeRetard,
        String coiffeurNom,
        String statut
) {
}
