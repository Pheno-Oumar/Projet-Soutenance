package com.kadi_aon.mon_salon.salon.dto;

import java.math.BigDecimal;

public record VarianteServiceDTOResponse(
        Long id,
        String nom,
        Integer dureeMinutes,
        BigDecimal prix,
        String imageUrl,
        Boolean statut
) {
    public VarianteServiceDTOResponse(
            Long id,
            String nom,
            Integer dureeMinutes,
            BigDecimal prix,
            Boolean statut
    ) {
        this(id, nom, dureeMinutes, prix, null, statut);
    }
}
