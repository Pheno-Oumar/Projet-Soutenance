package com.kadi_aon.mon_salon.salon.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.HoraireOuverture;

@Component
public class HoraireOuvertureDTOResponseMapper implements Function<HoraireOuverture, HoraireOuvertureDTOResponse> {

    @Override
    public HoraireOuvertureDTOResponse apply(HoraireOuverture h) {
        if (h == null) {
            return null;
        }
        return new HoraireOuvertureDTOResponse(
                h.getId(),
                h.getJourSemaine(),
                h.getHeureOuverture(),
                h.getHeureFermeture(),
                h.getPauseDebut(),
                h.getPauseFin(),
                h.getActif()
        );
    }
}
