package com.kadi_aon.mon_salon.kpi.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiPlateformeDTOResponse {

    private long nombreSalonsTotal;
    private long nombreSalonsActifs;
    private long nombreSalonsInactifs;

    private long nombreComptesTotal;
    private long nombreComptesActifs;
    private long nombreComptesInactifs;

    private long nombreRendezVousTotal;
    private long nombrePrestationsTotal;
    private long nombrePrestationsTerminees;

    private BigDecimal chiffreAffairesGlobal;
}
