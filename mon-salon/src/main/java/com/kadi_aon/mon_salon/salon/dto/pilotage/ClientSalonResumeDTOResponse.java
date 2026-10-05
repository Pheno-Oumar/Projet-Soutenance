package com.kadi_aon.mon_salon.salon.dto.pilotage;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSalonResumeDTOResponse {

    private Long clientId;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateNaissance;
    private LocalDateTime dateInscription;
    private Boolean hasProfilCapillaire;
}
