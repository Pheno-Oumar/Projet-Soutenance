package com.kadi_aon.mon_salon.profilcapillaire.dto;

import java.time.LocalDateTime;

public record ProfilCapillaireDTOResponse(
        Long id,
        Long compteId,
        String nomClient,
        String prenomClient,
        String typeCheveux,
        String texture,
        String longueur,
        String densite,
        String cuirChevelu,
        String etatCheveux,
        String sensibilites,
        String allergiesProduits,
        String observations,
        boolean hasCodeProfil,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour
) {
}
