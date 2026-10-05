package com.kadi_aon.mon_salon.profilcapillaire.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.entity.ProfilCapillaire;

@Component
public class ProfilCapillaireDTOResponseMapper implements Function<ProfilCapillaire, ProfilCapillaireDTOResponse> {

    @Override
    public ProfilCapillaireDTOResponse apply(ProfilCapillaire profil) {
        if (profil == null) {
            return null;
        }

        Long compteId = null;
        String nomClient = null;
        String prenomClient = null;

        if (profil.getCompte() != null) {
            compteId = profil.getCompte().getId();
            nomClient = profil.getCompte().getNom();
            prenomClient = profil.getCompte().getPrenom();
        }

        boolean hasCode = profil.getCodeProfil() != null && !profil.getCodeProfil().isBlank();

        return new ProfilCapillaireDTOResponse(
                profil.getId(),
                compteId,
                nomClient,
                prenomClient,
                profil.getTypeCheveux(),
                profil.getTexture(),
                profil.getLongueur(),
                profil.getDensite(),
                profil.getCuirChevelu(),
                profil.getEtatCheveux(),
                profil.getSensibilites(),
                profil.getAllergiesProduits(),
                profil.getObservations(),
                hasCode,
                profil.getDateCreation(),
                profil.getDateMiseAJour()
        );
    }
}
