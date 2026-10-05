package com.kadi_aon.mon_salon.salon.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kadi_aon.mon_salon.common.deserializer.FlexibleLocalDateTimeDeserializer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record VitrineRendezVousDTORequest(
        @NotNull(message = "La date et l'heure prévues sont obligatoires")
        @Future(message = "La date et l'heure du rendez-vous doivent être dans le futur")
        @JsonAlias({"dateHeure", "dateHeurePrevue"})
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime dateHeurePrevue,

        @NotEmpty(message = "Vous devez sélectionner au moins une prestation")
        @JsonAlias({"serviceIds", "varianteIds", "varianteServiceIds"})
        List<Long> varianteIds,

        Long coiffeurId,

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "Format d'adresse email invalide")
        String email,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        String telephone,

        String password,

        String notes
) {}
