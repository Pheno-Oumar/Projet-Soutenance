package com.kadi_aon.mon_salon.avis.dto;

import java.time.LocalDateTime;

public record AvisPrestationDTOResponse(
        Long id,
        Long lignePrestationId,
        Long prestationId,
        String serviceNom,
        String varianteNom,
        Long coiffeurId,
        String coiffeurNomComplet,
        Long clientId,
        String clientNomComplet,
        Integer note,
        String commentaire,
        boolean statut,
        LocalDateTime dateCreation,
        LocalDateTime dateModification
) {}
