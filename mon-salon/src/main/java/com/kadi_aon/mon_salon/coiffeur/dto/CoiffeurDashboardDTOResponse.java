package com.kadi_aon.mon_salon.coiffeur.dto;

import java.util.List;

import com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoiffeurDashboardDTOResponse {

    // Profil du coiffeur
    private Long affectationId;
    private String nomAffichage;
    private String biographie;
    private Integer anneeExperience;
    private String photoProfilUrl;

    // Métriques clés
    private long rdvAujourdhuiTotal;
    private long rdvAujourdhuiTermines;
    private long rdvAujourdhuiEnAttente;
    private long prestationsMoisTerminees;
    private Double noteMoyenne;
    private long totalAvis;

    // Planning du jour
    private List<PlanningRendezVousDTOResponse> planningAujourdhui;

    // Prochaines indisponibilités / congés prévus
    private List<IndisponibiliteDTOResponse> prochainesIndisponibilites;
}
