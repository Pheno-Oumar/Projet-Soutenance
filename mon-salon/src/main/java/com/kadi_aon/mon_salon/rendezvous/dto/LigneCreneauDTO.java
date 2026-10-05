package com.kadi_aon.mon_salon.rendezvous.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record LigneCreneauDTO(
                Long varianteId,
                String varianteNom,
                Long coiffeurAffectationId,
                String coiffeurNom,
                String coiffeurPrenom,
                LocalTime heureDebut,
                LocalTime heureFin,
                Integer dureeMinutes,
                BigDecimal prix) {
}
