package com.kadi_aon.mon_salon.rendezvous.dto;

import jakarta.validation.constraints.NotBlank;

public record RetardNotificationDTORequest(
        @NotBlank(message = "Le message de notification ne peut pas être vide")
        String message
) {
}
