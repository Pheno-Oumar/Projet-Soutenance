package com.kadi_aon.mon_salon.rendezvous.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.rendezvous.dto.LigneRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.entity.LigneRendezVous;

@Component
public class LigneRendezVousDTOResponseMapper implements Function<LigneRendezVous, LigneRendezVousDTOResponse> {

    @Override
    public LigneRendezVousDTOResponse apply(LigneRendezVous ligne) {
        if (ligne == null) {
            return null;
        }

        Long varianteId = ligne.getVarianteService() != null ? ligne.getVarianteService().getId() : null;
        String varianteNom = ligne.getVarianteService() != null ? ligne.getVarianteService().getNom() : null;

        return new LigneRendezVousDTOResponse(
                ligne.getId(),
                varianteId,
                varianteNom,
                ligne.getDateHeureDebut(),
                ligne.getDateHeureFin(),
                ligne.getDuree(),
                ligne.getPrix(),
                ligne.getOrdre()
        );
    }
}
