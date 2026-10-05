package com.kadi_aon.mon_salon.regle.dto;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.regle.entity.RegleSalon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegleSalonDTOResponse {

    private Long id;
    private String titre;
    private String description;
    private String salonSlug;
    private String salonNom;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    public static RegleSalonDTOResponse fromEntity(RegleSalon regle) {
        if (regle == null) {
            return null;
        }
        return RegleSalonDTOResponse.builder()
                .id(regle.getId())
                .titre(regle.getTitre())
                .description(regle.getDescription())
                .salonSlug(regle.getSalon() != null ? regle.getSalon().getSlug() : null)
                .salonNom(regle.getSalon() != null ? regle.getSalon().getNom() : null)
                .dateCreation(regle.getDateCreation())
                .dateModification(regle.getDateModification())
                .build();
    }
}
