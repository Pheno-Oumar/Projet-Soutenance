package com.kadi_aon.mon_salon.realisation.dto;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.realisation.enums.StatutCommentaire;

public record CommentaireDTOResponse(
        Long id,
        Long realisationId,
        Long auteurId,
        String auteurNomComplet,
        String auteurPhotoUrl,
        String contenu,
        StatutCommentaire statut,
        LocalDateTime dateCreation,
        boolean isMine
) {}
