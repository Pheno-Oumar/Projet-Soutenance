package com.kadi_aon.mon_salon.rendezvous.dto;

import com.kadi_aon.mon_salon.rendezvous.enums.ActionTraitementRetard;

import jakarta.validation.constraints.NotNull;

public record RetardTraitementDTORequest(
        @NotNull(message = "L'action de traitement est obligatoire")
        ActionTraitementRetard action,

        Integer minutesDecalage,

        String motif
) {
}
