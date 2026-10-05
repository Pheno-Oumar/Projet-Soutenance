package com.kadi_aon.mon_salon.account.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.entity.Compte;

@Component
public class CompteDTOResponseMapper implements Function<Compte, CompteDTOResponse> {

    @Override
    public CompteDTOResponse apply(Compte compte) {
        if (compte == null) {
            return null;
        }

        String rolePlateforme = (compte.getRolePlateforme() != null)
                ? compte.getRolePlateforme().getRole().name()
                : null;

        return new CompteDTOResponse(
                compte.getId(),
                compte.getNom(),
                compte.getPrenom(),
                compte.getDateNaissance(),
                compte.getEmail(),
                compte.getTelephone(),
                compte.getStatut(),
                rolePlateforme,
                compte.getDateCreation()
        );
    }
}
