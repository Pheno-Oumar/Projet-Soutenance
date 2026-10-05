package com.kadi_aon.mon_salon.rendezvous.mapper;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.rendezvous.dto.LigneRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RendezVousDTOResponseMapper implements Function<RendezVous, RendezVousDTOResponse> {

    private final LigneRendezVousDTOResponseMapper ligneMapper;

    @Override
    public RendezVousDTOResponse apply(RendezVous rdv) {
        if (rdv == null) {
            return null;
        }

        String salonSlug = rdv.getSalon() != null ? rdv.getSalon().getSlug() : null;
        String salonNom = rdv.getSalon() != null ? rdv.getSalon().getNom() : null;

        Long coiffeurAffectationId = rdv.getCoiffeur() != null ? rdv.getCoiffeur().getId() : null;
        String coiffeurNom = (rdv.getCoiffeur() != null && rdv.getCoiffeur().getCompte() != null)
                ? rdv.getCoiffeur().getCompte().getNom() : null;
        String coiffeurPrenom = (rdv.getCoiffeur() != null && rdv.getCoiffeur().getCompte() != null)
                ? rdv.getCoiffeur().getCompte().getPrenom() : null;

        String nomClient = rdv.getClient() != null ? rdv.getClient().getNom() : null;
        String prenomClient = rdv.getClient() != null ? rdv.getClient().getPrenom() : null;
        String telephoneClient = rdv.getClient() != null ? rdv.getClient().getTelephone() : null;

        List<LigneRendezVousDTOResponse> lignes = (rdv.getLignes() != null)
                ? rdv.getLignes().stream().map(ligneMapper).toList()
                : Collections.emptyList();

        return new RendezVousDTOResponse(
                rdv.getId(),
                salonSlug,
                salonNom,
                coiffeurAffectationId,
                coiffeurNom,
                coiffeurPrenom,
                nomClient,
                prenomClient,
                telephoneClient,
                rdv.getDateHeurePrevue(),
                rdv.getDateHeureFin(),
                rdv.getStatut() != null ? rdv.getStatut().name() : null,
                rdv.getMontantEstime(),
                rdv.getDateCreation(),
                lignes
        );
    }
}
