package com.kadi_aon.mon_salon.controller.proprietaire;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.depense.dto.DepenseDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.ExportTelechargementDTO;
import com.kadi_aon.mon_salon.facturation.dto.KpiFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportExportDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierFiltreDTORequest;
import com.kadi_aon.mon_salon.facturation.service.RapportFinancierSalonService;
import com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.FicheClientCompleteDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.KpiSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.PerformanceCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.StockSyntheseDTOResponse;
import com.kadi_aon.mon_salon.salon.service.ProprietairePilotageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/proprietaire")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'PROPRIETAIRE')")
@RequiredArgsConstructor
@Tag(name = "Propriétaire - Pilotage & Reporting", description = "KPIs, performances des coiffeurs, finances, stock et fiches clients")
public class ProprietairePilotageController {

    private final ProprietairePilotageService proprietairePilotageService;
    private final RapportFinancierSalonService rapportFinancierSalonService;

    @GetMapping("/performances-coiffeurs")
    @Operation(summary = "Consulter les performances de chaque coiffeur du salon (prestations réalisées et CA généré)")
    public ResponseEntity<APIResponse<List<PerformanceCoiffeurDTOResponse>>> getPerformancesCoiffeurs(
            @PathVariable String slugSalon) {
        List<PerformanceCoiffeurDTOResponse> performances = proprietairePilotageService.obtenirPerformancesCoiffeurs(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Performances des coiffeurs récupérées avec succès", performances));
    }

    @GetMapping("/kpi")
    @Operation(summary = "Consulter les indicateurs clés de performance (KPI) du salon")
    public ResponseEntity<APIResponse<KpiSalonDTOResponse>> getKpiSalon(
            @PathVariable String slugSalon) {
        KpiSalonDTOResponse kpi = proprietairePilotageService.obtenirKpiSalon(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "KPIs du salon récupérés avec succès", kpi));
    }

    @GetMapping("/revenus")
    @Operation(summary = "Consulter le détail des revenus et paiements encaissés par le salon")
    public ResponseEntity<APIResponse<List<PaiementDTOResponse>>> getRevenus(
            @PathVariable String slugSalon) {
        List<PaiementDTOResponse> revenus = proprietairePilotageService.obtenirRevenusDetailles(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Revenus détaillés récupérés avec succès", revenus));
    }

    @GetMapping("/depenses")
    @Operation(summary = "Consulter le détail des dépenses du salon")
    public ResponseEntity<APIResponse<List<DepenseDTOResponse>>> getDepenses(
            @PathVariable String slugSalon) {
        List<DepenseDTOResponse> depenses = proprietairePilotageService.obtenirDepensesDetailles(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Dépenses détaillées récupérées avec succès", depenses));
    }

    @GetMapping("/stock")
    @Operation(summary = "Consulter l'état de synthèse des stocks du salon et les alertes")
    public ResponseEntity<APIResponse<List<StockSyntheseDTOResponse>>> getStock(
            @PathVariable String slugSalon) {
        List<StockSyntheseDTOResponse> stock = proprietairePilotageService.obtenirSyntheseStock(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Synthèse des stocks récupérée avec succès", stock));
    }

    @GetMapping("/clients")
    @Operation(summary = "Lister les clients enregistrés dans le salon")
    public ResponseEntity<APIResponse<List<ClientSalonResumeDTOResponse>>> getClients(
            @PathVariable String slugSalon) {
        List<ClientSalonResumeDTOResponse> clients = proprietairePilotageService.listerClientsDuSalon(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Clients du salon récupérés avec succès", clients));
    }

    @GetMapping("/clients/{clientId}")
    @Operation(summary = "Consulter la fiche client complète (profil capillaire, RDVs, prestations, factures, paiements)")
    public ResponseEntity<APIResponse<FicheClientCompleteDTOResponse>> getFicheClientComplete(
            @PathVariable String slugSalon,
            @PathVariable Long clientId) {
        FicheClientCompleteDTOResponse fiche = proprietairePilotageService.obtenirFicheClientComplete(slugSalon, clientId);
        return ResponseEntity.ok(new APIResponse<>(true, "Fiche client complète récupérée avec succès", fiche));
    }

    // --- RAPPORTS FINANCIERS DU PROPRIÉTAIRE ---

    @PostMapping("/rapports/consulter")
    @Operation(summary = "Consulter un rapport financier en direct par le propriétaire (entrées, sorties, filtre par date ou catégorie)")
    public ResponseEntity<APIResponse<RapportFinancierDTOResponse>> consulterRapport(
            @PathVariable String slugSalon,
            @Valid @RequestBody RapportFinancierFiltreDTORequest filtre) {
        RapportFinancierDTOResponse response = rapportFinancierSalonService.consulterRapportFinancier(slugSalon, filtre);
        return ResponseEntity.ok(new APIResponse<>(true, "Rapport financier généré avec succès", response));
    }

    @PostMapping("/rapports/exporter")
    @Operation(summary = "Générer et exporter un rapport financier par le propriétaire (PDF, CSV, JSON)")
    public ResponseEntity<APIResponse<RapportExportDTOResponse>> exporterRapport(
            @PathVariable String slugSalon,
            @Valid @RequestBody RapportFinancierFiltreDTORequest filtre,
            Principal principal) {
        RapportExportDTOResponse response = rapportFinancierSalonService.exporterRapportFinancier(
                slugSalon, principal.getName(), filtre);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Rapport financier exporté avec succès", response));
    }

    @GetMapping("/rapports/kpi-financiers")
    @Operation(summary = "Consulter les indicateurs clés financiers (KPI) globaux du salon par le propriétaire")
    public ResponseEntity<APIResponse<KpiFinancierDTOResponse>> getKpiFinanciers(
            @PathVariable String slugSalon) {
        KpiFinancierDTOResponse response = rapportFinancierSalonService.obtenirKpiFinanciers(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "KPIs financiers récupérés avec succès", response));
    }

    @GetMapping("/rapports/exports/{exportId}/telecharger")
    @Operation(summary = "Télécharger directement un rapport financier exporté par le propriétaire (PDF, CSV, JSON)")
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
