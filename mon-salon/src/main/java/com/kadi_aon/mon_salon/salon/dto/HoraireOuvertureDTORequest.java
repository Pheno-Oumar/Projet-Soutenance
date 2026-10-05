package com.kadi_aon.mon_salon.salon.dto;

import java.time.LocalTime;

import com.kadi_aon.mon_salon.salon.enums.JourSemaine;

import jakarta.validation.constraints.NotNull;

public record HoraireOuvertureDTORequest(
        @NotNull(message = "Le jour de la semaine est obligatoire")
        JourSemaine jourSemaine,

        @NotNull(message = "L'heure d'ouverture est obligatoire")
        LocalTime heureOuverture,

        @NotNull(message = "L'heure de fermeture est obligatoire")
        LocalTime heureFermeture,

        LocalTime pauseDebut,
        LocalTime pauseFin,
        Boolean actif
) {}
