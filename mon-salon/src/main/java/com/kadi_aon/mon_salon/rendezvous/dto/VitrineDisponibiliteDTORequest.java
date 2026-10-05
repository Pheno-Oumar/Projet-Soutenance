package com.kadi_aon.mon_salon.rendezvous.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record VitrineDisponibiliteDTORequest(
        @NotNull(message = "La date de recherche est obligatoire")
        @FutureOrPresent(message = "La date doit être aujourd'hui ou dans le futur")
        LocalDate date,

        @NotEmpty(message = "Vous devez sélectionner au moins une variante de service")
        List<Long> varianteIds,

        LocalTime heureMinimale
) {
    public DisponibiliteSearchDTORequest toDisponibiliteSearch() {
        return new DisponibiliteSearchDTORequest(date, varianteIds, null, heureMinimale);
    }
}
