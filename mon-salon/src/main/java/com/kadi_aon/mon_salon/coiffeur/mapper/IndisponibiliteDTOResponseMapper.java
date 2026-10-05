package com.kadi_aon.mon_salon.coiffeur.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.entity.IndisponibiliteCoiffeur;

@Component
public class IndisponibiliteDTOResponseMapper implements Function<IndisponibiliteCoiffeur, IndisponibiliteDTOResponse> {

    @Override
    public IndisponibiliteDTOResponse apply(IndisponibiliteCoiffeur entity) {
        if (entity == null) {
            return null;
        }

        Long affectationId = entity.getCoiffeur() != null ? entity.getCoiffeur().getId() : null;

        return new IndisponibiliteDTOResponse(
                entity.getId(),
                affectationId,
                entity.getDateDebut(),
                entity.getDateFin(),
                entity.getMotif() != null ? entity.getMotif().name() : null,
                entity.getCommentaire(),
                entity.getDateCreation()
        );
    }
}
