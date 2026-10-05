package com.kadi_aon.mon_salon.avis.dto;

import java.time.LocalDateTime;

public record AvisSalonDTOResponse(
        Long id,
        String salonSlug,
        String salonNom,
        Long clientId,
        String clientNomComplet,
        Integer note,
        String commentaire,
        boolean statut,
        LocalDateTime dateCreation,
        LocalDateTime dateModification
) {}
