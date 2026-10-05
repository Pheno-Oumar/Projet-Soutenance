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
public class StockSyntheseDTOResponse {

    private Long produitId;
    private String produitNom;
    private String categorieNom;
    private BigDecimal prixVente;
    private Integer quantiteDisponible;
    private Integer seuilMinimum;
    private Integer seuilMaximum;
    private boolean alerteStockBas;
}
