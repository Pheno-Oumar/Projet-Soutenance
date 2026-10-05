package com.kadi_aon.mon_salon.salon.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.salon.dto.FermetureExceptionnelleDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.FermetureExceptionnelle;

@Component
public class FermetureExceptionnelleDTOResponseMapper implements Function<FermetureExceptionnelle, FermetureExceptionnelleDTOResponse> {

    @Override
    public FermetureExceptionnelleDTOResponse apply(FermetureExceptionnelle f) {
        if (f == null) {
            return null;
        }
        return new FermetureExceptionnelleDTOResponse(
                f.getId(),
                f.getDateDebut(),
                f.getDateFin(),
                f.getMotif(),
                f.getDateCreation()
        );
    }
}
