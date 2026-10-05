package com.kadi_aon.mon_salon.salon.dto.pilotage;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceCoiffeurDTOResponse {

    private Long coiffeurId;
    private String nom;
    private String prenom;
    private String email;
    private long nombrePrestations;
    private BigDecimal chiffreAffaires;
}
