package com.kadi_aon.mon_salon.stock.dto;

import java.util.List;

public record CategorieProduitDTOResponse(
        Long id,
        String nom,
        String description,
        String imageUrl,
        boolean statut,
        String salonSlug,
        int nombreProduits,
        List<ProduitDTOResponse> produits
) {
    public CategorieProduitDTOResponse(
            Long id,
            String nom,
            String description,
            boolean statut,
            String salonSlug,
            int nombreProduits,
            List<ProduitDTOResponse> produits
    ) {
        this(id, nom, description, null, statut, salonSlug, nombreProduits, produits);
    }
}
