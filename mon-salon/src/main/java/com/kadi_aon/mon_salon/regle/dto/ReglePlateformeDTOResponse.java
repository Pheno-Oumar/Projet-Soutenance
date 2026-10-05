package com.kadi_aon.mon_salon.regle.dto;

import java.time.LocalDateTime;

import com.kadi_aon.mon_salon.regle.entity.ReglePlateforme;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReglePlateformeDTOResponse {

    private Long id;
    private String titre;
    private String description;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    public static ReglePlateformeDTOResponse fromEntity(ReglePlateforme regle) {
        if (regle == null) {
            return null;
        }
        return ReglePlateformeDTOResponse.builder()
                .id(regle.getId())
                .titre(regle.getTitre())
                .description(regle.getDescription())
                .actif(regle.getActif())
                .dateCreation(regle.getDateCreation())
                .dateModification(regle.getDateModification())
                .build();
    }
}
