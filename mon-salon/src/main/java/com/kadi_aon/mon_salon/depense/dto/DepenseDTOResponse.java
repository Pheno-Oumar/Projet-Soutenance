package com.kadi_aon.mon_salon.depense.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DepenseDTOResponse(
        Long id,
        BigDecimal montant,
        LocalDateTime dateDepense,
        String description,
        String categorie,
        boolean statut,
        String salonSlug,
        String comptableNomComplet,
        Long operationCaisseId,
        Long mouvementStockId
) {}
