package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PanierDTOResponse(
        Long id,
        String slugSalon,
        String clientEmail,
        List<LignePanierDTOResponse> lignes,
        BigDecimal montantTotal,
        int nombreArticles,
        LocalDateTime dateModification
) {}
