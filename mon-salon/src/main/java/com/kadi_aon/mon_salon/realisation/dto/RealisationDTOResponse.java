package com.kadi_aon.mon_salon.realisation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RealisationDTOResponse(
        Long id,
        String titre,
        String description,
        String urlVideo,
        LocalDate dateRealisation,
        LocalDateTime datePublication,
        boolean statutPublication,
        String salonSlug,
        String salonNom,
        Long coiffeurId,
        String coiffeurNomComplet,
        int totalLikes,
        int totalCommentaires,
        long totalVues,
        LocalDateTime dateCreation,
        LocalDateTime dateModification
) {}
