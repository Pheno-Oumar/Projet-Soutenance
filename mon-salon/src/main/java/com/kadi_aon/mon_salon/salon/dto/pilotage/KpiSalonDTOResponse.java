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
public class KpiSalonDTOResponse {

    private String slugSalon;
    private String nomSalon;

    private long nombreClients;
    private long nombreCoiffeurs;

    private long nombreRendezVousTotal;
    private long nombreRendezVousTermines;
    private long nombreRendezVousAnnules;

    private long nombrePrestationsTotal;
    private long nombrePrestationsTerminees;

    private BigDecimal totalRevenus;
    private BigDecimal totalDepenses;
    private BigDecimal beneficeNet;
}
