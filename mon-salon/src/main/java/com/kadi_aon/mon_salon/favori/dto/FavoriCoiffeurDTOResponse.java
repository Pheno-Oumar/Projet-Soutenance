package com.kadi_aon.mon_salon.favori.dto;

import java.time.LocalDateTime;

public record FavoriCoiffeurDTOResponse(
        Long id,
        Long coiffeurId,
        String coiffeurNomComplet,
        String coiffeurPhotoUrl,
        String salonSlug,
        String salonNom,
        Long clientId,
        LocalDateTime dateAjout
) {}
