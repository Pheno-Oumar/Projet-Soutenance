package com.kadi_aon.mon_salon.prestation.dto;

import java.util.List;

import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record PrestationCreateDTORequest(
        Long rendezVousId,
        Long coiffeurAffectationId,
        Long clientCompteId,
        String nomClient,
        String prenomClient,
        String telephoneClient,
        StatutPrestation statut,

        @NotEmpty(message = "La prestation doit comporter au moins une ligne de service")
        List<@Valid LignePrestationDTORequest> lignes
) {
    public PrestationCreateDTORequest(
            Long rendezVousId,
            Long coiffeurAffectationId,
            Long clientCompteId,
            String nomClient,
            String prenomClient,
            String telephoneClient,
            List<LignePrestationDTORequest> lignes
    ) {
        this(rendezVousId, coiffeurAffectationId, clientCompteId, nomClient, prenomClient, telephoneClient, null, lignes);
    }
}
