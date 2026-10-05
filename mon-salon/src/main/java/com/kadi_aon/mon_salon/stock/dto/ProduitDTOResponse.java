package com.kadi_aon.mon_salon.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProduitDTOResponse(
        Long id,
        String nom,
        String description,
        BigDecimal prixVente,
        String imageUrl,
        boolean statut,
        Long categorieId,
        String categorieNom,
        StockProduitDTOResponse stock,
        LocalDateTime dateCreation,
        LocalDateTime dateModification
) {
    public ProduitDTOResponse(
            Long id,
            String nom,
            String description,
            BigDecimal prixVente,
            boolean statut,
            Long categorieId,
            String categorieNom,
            StockProduitDTOResponse stock,
            LocalDateTime dateCreation,
            LocalDateTime dateModification
    ) {
        this(id, nom, description, prixVente, null, statut, categorieId, categorieNom, stock, dateCreation, dateModification);
    }
}
