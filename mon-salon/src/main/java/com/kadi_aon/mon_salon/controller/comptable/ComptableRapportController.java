package com.kadi_aon.mon_salon.controller.comptable;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.facturation.dto.ExportTelechargementDTO;
import com.kadi_aon.mon_salon.facturation.dto.KpiFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportExportDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierFiltreDTORequest;
import com.kadi_aon.mon_salon.facturation.service.RapportFinancierSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/comptable/rapports")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'COMPTABLE')")
@RequiredArgsConstructor
@Tag(name = "Espace Comptable - Rapports & KPI", description = "Consultation et génération des rapports financiers (PDF, CSV, JSON) et indicateurs financiers")
public class ComptableRapportController {

    private final RapportFinancierSalonService rapportFinancierSalonService;

    @PostMapping("/consulter")
    @Operation(summary = "Consulter un rapport financier en direct (entrées, sorties, filtre par date ou catégorie)")
    public ResponseEntity<APIResponse<RapportFinancierDTOResponse>> consulterRapport(
            @PathVariable String slugSalon,
            @Valid @RequestBody RapportFinancierFiltreDTORequest filtre) {

        RapportFinancierDTOResponse response = rapportFinancierSalonService.consulterRapportFinancier(slugSalon, filtre);
        return ResponseEntity.ok(new APIResponse<>(true, "Rapport financier généré avec succès", response));
    }

    @PostMapping("/exporter")
    @Operation(summary = "Générer et exporter un rapport financier (PDF, CSV, JSON) téléversé sur Cloudinary avec expiration automatique sous 24h")
    public ResponseEntity<APIResponse<RapportExportDTOResponse>> exporterRapport(
            @PathVariable String slugSalon,
            @Valid @RequestBody RapportFinancierFiltreDTORequest filtre,
            Principal principal) {

        RapportExportDTOResponse response = rapportFinancierSalonService.exporterRapportFinancier(
                slugSalon, principal.getName(), filtre);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Rapport financier exporté avec succès (valable 24 heures)", response));
    }

    @GetMapping("/kpi")
    @Operation(summary = "Consulter les indicateurs clés financiers (KPI) du salon")
    public ResponseEntity<APIResponse<KpiFinancierDTOResponse>> getKpiFinanciers(
            @PathVariable String slugSalon) {

        KpiFinancierDTOResponse response = rapportFinancierSalonService.obtenirKpiFinanciers(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "KPIs financiers récupérés avec succès", response));
    }

    @GetMapping("/mes-exports")
    @Operation(summary = "Lister l'historique des exports de rapports financiers générés")
    public ResponseEntity<APIResponse<List<RapportExportDTOResponse>>> listerMesExports(
            @PathVariable String slugSalon,
            Principal principal) {

        List<RapportExportDTOResponse> response = rapportFinancierSalonService.listerMesExportsFinanciers(
                principal.getName(), slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Exports financiers récupérés avec succès", response));
    }

    @GetMapping("/exports/{exportId}/telecharger")
    @Operation(summary = "Télécharger directement un rapport financier exporté (PDF, CSV, JSON) sans restriction Cloudinary")
    public ResponseEntity<byte[]> telechargerExport(
            @PathVariable String slugSalon,
            @PathVariable Long exportId,
            Principal principal) {

        ExportTelechargementDTO export = rapportFinancierSalonService.telechargerExport(slugSalon, exportId, principal.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, export.contentType())
                .body(export.data());
    }
}
