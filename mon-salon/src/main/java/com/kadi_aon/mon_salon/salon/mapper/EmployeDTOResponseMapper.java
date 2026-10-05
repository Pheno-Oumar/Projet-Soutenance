package com.kadi_aon.mon_salon.salon.mapper;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.salon.dto.EmployeDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;

@Component
public class EmployeDTOResponseMapper implements Function<AffectationSalon, EmployeDTOResponse> {

    @Override
    public EmployeDTOResponse apply(AffectationSalon affectation) {
        if (affectation == null) {
            return null;
        }

        Set<String> roleNames = (affectation.getRoles() != null)
                ? affectation.getRoles().stream().map(r -> r.getRole().name()).collect(Collectors.toSet())
                : Collections.emptySet();

        return new EmployeDTOResponse(
                affectation.getId(),
                affectation.getCompte() != null ? affectation.getCompte().getId() : null,
                affectation.getCompte() != null ? affectation.getCompte().getNom() : null,
                affectation.getCompte() != null ? affectation.getCompte().getPrenom() : null,
                affectation.getCompte() != null ? affectation.getCompte().getEmail() : null,
                affectation.getCompte() != null ? affectation.getCompte().getTelephone() : null,
                roleNames,
                affectation.getStatut(),
                affectation.getDateDebut(),
                affectation.getDateFin()
        );
    }
}
