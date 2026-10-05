package com.kadi_aon.mon_salon.reclamation.dto;

import java.time.LocalDateTime;

public record ReclamationDTOResponse(
        Long id,
        String objet,
        String description,
        String statut,
        String reponseTraitement,
        LocalDateTime dateCreation,
        LocalDateTime dateTraitement,
        String clientNomComplet,
        String clientEmail,
        String traiteParNomComplet,
        String salonSlug
) {}
