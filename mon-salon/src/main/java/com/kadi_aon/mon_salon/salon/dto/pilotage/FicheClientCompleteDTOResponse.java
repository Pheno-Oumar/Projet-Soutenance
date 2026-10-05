package com.kadi_aon.mon_salon.salon.dto.pilotage;

import java.time.LocalDate;
import java.util.List;

import com.kadi_aon.mon_salon.facturation.dto.FactureDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FicheClientCompleteDTOResponse {

    private Long clientId;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateNaissance;

    private ProfilCapillaireDTOResponse profilCapillaire;
    private List<RendezVousDTOResponse> rendezVous;
    private List<PrestationDTOResponse> prestations;
    private List<FactureDTOResponse> factures;
    private List<PaiementDTOResponse> paiements;
}
