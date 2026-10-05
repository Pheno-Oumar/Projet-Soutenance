package com.kadi_aon.mon_salon.salon.dto;

public record ReceptionnisteStatsJourDTO(
        long totalRendezVousJour,
        long rdvEnAttente,
        long prestationsEnCours,
        long prestationsTerminees,
        long clientsEnRetardCount,
        long noShowsCount
) {}
