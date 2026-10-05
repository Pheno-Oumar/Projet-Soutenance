package com.kadi_aon.mon_salon.stock.dto;

import java.time.LocalDateTime;

public record StockProduitDTOResponse(
        Long id,
        Integer quantiteDisponible,
        Integer seuilMinimum,
        Integer seuilMaximum,
        boolean alerteStockBas,
        LocalDateTime dateDerniereMiseAJour
) {}
