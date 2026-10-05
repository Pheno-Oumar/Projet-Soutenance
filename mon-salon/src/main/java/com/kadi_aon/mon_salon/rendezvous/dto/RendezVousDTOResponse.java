package com.kadi_aon.mon_salon.rendezvous.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RendezVousDTOResponse(
        Long id,
        String salonSlug,
        String salonNom,
        Long coiffeurAffectationId,
        String coiffeurNom,
        String coiffeurPrenom,
        String nomClient,
        String prenomClient,
        String telephoneClient,
        LocalDateTime dateHeurePrevue,
        LocalDateTime dateHeureFin,
        String statut,
        BigDecimal montantEstime,
        LocalDateTime dateCreation,
        List<LigneRendezVousDTOResponse> lignes
) {}
