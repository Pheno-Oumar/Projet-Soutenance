package com.kadi_aon.mon_salon.salon.dto;

import java.util.List;

import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardRendezVousDTOResponse;

public record ReceptionnisteDashboardDTOResponse(
        ReceptionnisteStatsJourDTO stats,
        ReceptionnisteCaisseStatutDTO caisse,
        List<PlanningRendezVousDTOResponse> prochainsRendezVous,
        List<RetardRendezVousDTOResponse> retardsActuels,
        List<PrestationDTOResponse> prestationsEnCours
) {}
