package com.kadi_aon.mon_salon.controller.admin_systeme;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.kpi.dto.KpiPlateformeDTOResponse;
import com.kadi_aon.mon_salon.kpi.service.KpiPlateformeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin-systeme/kpi")
@PreAuthorize("hasRole('ADMIN_SYSTEME')")
@RequiredArgsConstructor
@Tag(name = "Admin Système - Dashboard & KPIs", description = "Tableau de bord et indicateurs de performance de la plateforme")
public class AdminSystemeDashboardController {

    private final KpiPlateformeService kpiPlateformeService;

    @GetMapping
    @Operation(summary = "Consulter les indicateurs clés de performance (KPI) globaux de la plateforme")
    public ResponseEntity<APIResponse<KpiPlateformeDTOResponse>> getKpiPlateforme() {
        KpiPlateformeDTOResponse kpis = kpiPlateformeService.calculerKpisPlateforme();
        return ResponseEntity.ok(new APIResponse<>(true, "KPIs de la plateforme récupérés avec succès", kpis));
    }
}
