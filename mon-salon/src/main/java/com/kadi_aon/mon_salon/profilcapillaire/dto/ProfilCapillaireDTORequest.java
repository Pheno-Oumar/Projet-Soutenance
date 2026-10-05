package com.kadi_aon.mon_salon.profilcapillaire.dto;

import jakarta.validation.constraints.Size;

public record ProfilCapillaireDTORequest(
        @Size(max = 50, message = "Le type de cheveux ne doit pas dépasser 50 caractères")
        String typeCheveux,

        @Size(max = 50, message = "La texture ne doit pas dépasser 50 caractères")
        String texture,

        @Size(max = 50, message = "La longueur ne doit pas dépasser 50 caractères")
        String longueur,

        @Size(max = 50, message = "La densité ne doit pas dépasser 50 caractères")
        String densite,

        @Size(max = 50, message = "Le cuir chevelu ne doit pas dépasser 50 caractères")
        String cuirChevelu,

        @Size(max = 100, message = "L'état des cheveux ne doit pas dépasser 100 caractères")
        String etatCheveux,

        String sensibilites,

        String allergiesProduits,

        String observations
) {
}
