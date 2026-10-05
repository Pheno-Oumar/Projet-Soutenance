package com.kadi_aon.mon_salon.salon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceptionnisteCaisseStatutDTO(
        boolean caisseOuverte,
        Long sessionCaisseId,
        LocalDateTime dateOuvertureSession,
        BigDecimal soldeOuverture,
        BigDecimal totalEncaisseJour,
        int nombreOperationsJour
) {}
