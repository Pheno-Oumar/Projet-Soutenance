package com.kadi_aon.mon_salon.coiffeur.dto;

import java.time.LocalDateTime;

public record IndisponibiliteDTOResponse(
        Long id,
        Long coiffeurAffectationId,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        String motif,
        String commentaire,
        LocalDateTime dateCreation
) {}
