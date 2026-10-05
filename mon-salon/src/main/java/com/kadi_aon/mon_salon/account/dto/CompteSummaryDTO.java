package com.kadi_aon.mon_salon.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteSummaryDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String rolePlateforme;
}
