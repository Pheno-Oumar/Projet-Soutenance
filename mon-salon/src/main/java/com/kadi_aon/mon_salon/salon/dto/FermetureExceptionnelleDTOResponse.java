package com.kadi_aon.mon_salon.salon.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FermetureExceptionnelleDTOResponse(
        Long id,
        LocalDate dateDebut,
        LocalDate dateFin,
        String motif,
        LocalDateTime dateCreation
) {}
