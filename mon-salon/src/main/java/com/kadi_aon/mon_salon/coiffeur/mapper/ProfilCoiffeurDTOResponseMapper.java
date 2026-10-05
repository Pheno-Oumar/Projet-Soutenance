package com.kadi_aon.mon_salon.coiffeur.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.entity.ProfilCoiffeur;

@Component
public class ProfilCoiffeurDTOResponseMapper implements Function<ProfilCoiffeur, ProfilCoiffeurDTOResponse> {

    @Override
    public ProfilCoiffeurDTOResponse apply(ProfilCoiffeur profil) {
        if (profil == null) {
            return null;
        }

        String nom = null;
        String prenom = null;
        Long affectationId = null;

        if (profil.getAffectation() != null) {
            affectationId = profil.getAffectation().getId();
            if (profil.getAffectation().getCompte() != null) {
                nom = profil.getAffectation().getCompte().getNom();
                prenom = profil.getAffectation().getCompte().getPrenom();
            }
        }

        return new ProfilCoiffeurDTOResponse(
                profil.getId(),
                affectationId,
                nom,
                prenom,
                profil.getNomAffichage(),
                profil.getBiographie(),
                profil.getAnneeExperience(),
                profil.getPhotoProfilUrl(),
                profil.getDescription()
        );
    }
}
