package com.kadi_aon.mon_salon.coiffeur.dto;

public record ProfilCoiffeurDTOResponse(
        Long id,
        Long affectationId,
        String coiffeurNom,
        String coiffeurPrenom,
        String nomAffichage,
        String biographie,
        Integer anneeExperience,
        String photoProfilUrl,
        String description
) {}
