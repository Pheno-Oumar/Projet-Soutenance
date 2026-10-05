package com.kadi_aon.mon_salon.rendezvous.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kadi_aon.mon_salon.common.deserializer.FlexibleLocalDateTimeDeserializer;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RendezVousCreateDTORequest(
        @NotNull(message = "La date et l'heure prévues sont obligatoires")
        @Future(message = "La date et l'heure du rendez-vous doivent être dans le futur")
        @JsonAlias({"dateHeure", "dateHeurePrevue"})
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime dateHeurePrevue,

        @NotEmpty(message = "Vous devez sélectionner au moins une variante de service")
        @JsonAlias({"serviceIds", "varianteIds", "varianteServiceIds"})
        List<Long> varianteIds,

        Long coiffeurId
) {}
