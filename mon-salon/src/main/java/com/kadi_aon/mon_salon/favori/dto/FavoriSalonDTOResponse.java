package com.kadi_aon.mon_salon.favori.dto;

import java.time.LocalDateTime;

public record FavoriSalonDTOResponse(
        Long id,
        String salonSlug,
        String salonNom,
        String salonLogoUrl,
        Long clientId,
        LocalDateTime dateAjout
) {}
