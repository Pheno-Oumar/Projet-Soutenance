package com.kadi_aon.mon_salon.rendezvous.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LigneRendezVousDTOResponse(
        Long id,
        Long varianteId,
        String varianteNom,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        Integer duree,
        BigDecimal prix,
        Integer ordre
) {}
