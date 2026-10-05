package com.kadi_aon.mon_salon.salon.dto;

import java.time.LocalDate;
import java.util.Set;

public record EmployeDTOResponse(
        Long affectationId,
        Long compteId,
        String nom,
        String prenom,
        String email,
        String telephone,
        Set<String> roles,
        Boolean statut,
        LocalDate dateDebut,
        LocalDate dateFin
) {}
