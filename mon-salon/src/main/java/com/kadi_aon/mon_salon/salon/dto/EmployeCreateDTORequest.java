package com.kadi_aon.mon_salon.salon.dto;

import java.util.Set;

import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record EmployeCreateDTORequest(
        @NotBlank(message = "Le nom de l'employé est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom de l'employé est obligatoire")
        String prenom,

        @NotBlank(message = "L'email de l'employé est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        String telephone,

        @NotEmpty(message = "Au moins un rôle salon doit être attribué à l'employé")
        Set<TypeRoleSalon> roles
) {}
