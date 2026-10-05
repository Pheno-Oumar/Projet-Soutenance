package com.kadi_aon.mon_salon.facturation.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.kadi_aon.mon_salon.depense.enums.CategorieDepense;
import com.kadi_aon.mon_salon.facturation.enums.TypePaiement;
import com.kadi_aon.mon_salon.facturation.enums.TypeRapportFinancier;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RapportFinancierFiltreDTORequest(
        @Schema(description = "Date de début de la période d'analyse (par défaut début du mois)", example = "2026-09-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateDebut,

        @Schema(description = "Date de fin de la période d'analyse (par défaut aujourd'hui)", example = "2026-09-22")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateFin,

        @NotNull(message = "Le type de rapport est obligatoire.")
        @Schema(description = "Type de rapport financier souhaité", example = "ENTREES_SORTIES")
        TypeRapportFinancier typeRapport,

        @Schema(description = "Catégorie de dépense (obligatoire si typeRapport = DEPENSES_CATEGORIE)", example = "LOYER")
        CategorieDepense categorieDepense,

        @Schema(description = "Type de paiement pour entrée spécifique (si typeRapport = ENTREE_SPECIFIQUE)", example = "ESPECES")
        TypePaiement typePaiement,

        @Schema(description = "Format d'export souhaité : PDF, CSV ou JSON", example = "PDF")
        FormatExportDonnees format
) {}
