package com.kadi_aon.mon_salon.prestation.dto;

import java.math.BigDecimal;

public record LignePrestationDTOResponse(
        Long id,
        Long varianteServiceId,
        String nomVariante,
        BigDecimal prixReel
) {}
