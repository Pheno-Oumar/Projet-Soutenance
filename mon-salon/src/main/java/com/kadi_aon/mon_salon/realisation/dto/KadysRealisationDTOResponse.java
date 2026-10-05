package com.kadi_aon.mon_salon.realisation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record KadysRealisationDTOResponse(
        Long id,
        String titre,
        String description,
        String urlVideo,
        LocalDate dateRealisation,
        LocalDateTime datePublication,
        String salonSlug,
        String salonNom,
        String salonLogoUrl,
        Long coiffeurId,
        String coiffeurNomComplet,
        int totalLikes,
        int totalCommentaires,
        long totalVues,
        boolean isLikedByCurrentUser
) {}
