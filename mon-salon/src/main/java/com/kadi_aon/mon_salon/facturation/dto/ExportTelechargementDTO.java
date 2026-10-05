package com.kadi_aon.mon_salon.facturation.dto;

public record ExportTelechargementDTO(
        byte[] data,
        String filename,
        String contentType
) {}
