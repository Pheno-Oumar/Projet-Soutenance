package com.kadi_aon.mon_salon.salon.dto;

public record SalonUpdateDTORequest(
        String nom,
        String description,
        String adresse,
        String telephone,
        String email,
        Double latitude,
        Double longitude
) {}
