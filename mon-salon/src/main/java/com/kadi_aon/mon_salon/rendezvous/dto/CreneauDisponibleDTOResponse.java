package com.kadi_aon.mon_salon.rendezvous.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record CreneauDisponibleDTOResponse(
                LocalTime heureDebut,
                LocalTime heureFin,
                Integer dureeTotale,
                BigDecimal montantTotal,
                List<LigneCreneauDTO> lignesProposees) {
}
