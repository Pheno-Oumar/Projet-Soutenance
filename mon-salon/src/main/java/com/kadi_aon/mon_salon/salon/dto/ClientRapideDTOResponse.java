package com.kadi_aon.mon_salon.salon.dto;

import java.time.LocalDate;

public record ClientRapideDTOResponse(
        Long id,
        String nom,
        String prenom,
        String telephone,
        String email,
        LocalDate dateNaissance
) {
}
