package com.kadi_aon.mon_salon.caisse.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperationCaisseDTOResponse(
        Long id,
        BigDecimal montant,
        LocalDateTime dateOperation,
        String libelle,
        String type,
        Boolean statut,
        String numeroPaiement
) {}
