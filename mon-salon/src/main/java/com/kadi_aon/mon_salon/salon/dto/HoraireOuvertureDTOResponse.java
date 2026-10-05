package com.kadi_aon.mon_salon.salon.dto;

import java.time.LocalTime;

import com.kadi_aon.mon_salon.salon.enums.JourSemaine;

public record HoraireOuvertureDTOResponse(
        Long id,
        JourSemaine jourSemaine,
        LocalTime heureOuverture,
        LocalTime heureFermeture,
        LocalTime pauseDebut,
        LocalTime pauseFin,
        Boolean actif
) {}
