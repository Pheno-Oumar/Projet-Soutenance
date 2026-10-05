package com.kadi_aon.mon_salon.account.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record CompteUpdateDTORequest(
        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        LocalDate dateNaissance,

        @NotBlank(message = "Le téléphone est obligatoire")
        String telephone
) {}
