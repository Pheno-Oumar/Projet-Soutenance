package com.kadi_aon.mon_salon.profilcapillaire.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CodeProfilDTORequest(
        @NotBlank(message = "Le code PIN est obligatoire")
        @Pattern(regexp = "^[0-9]{6}$", message = "Le code d'accès doit être composé d'exactement 6 chiffres")
        String codePin
) {
}
