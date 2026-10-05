package com.kadi_aon.mon_salon.controller.admin_systeme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.kpi.dto.KpiPlateformeDTOResponse;
import com.kadi_aon.mon_salon.kpi.service.KpiPlateformeService;

@ExtendWith(MockitoExtension.class)
class AdminSystemeDashboardControllerTest {

    @Mock
    private KpiPlateformeService kpiPlateformeService;

    @InjectMocks
    private AdminSystemeDashboardController adminSystemeDashboardController;

    @Test
    void getKpiPlateforme() {
        KpiPlateformeDTOResponse dto = KpiPlateformeDTOResponse.builder()
                .nombreSalonsTotal(5)
                .chiffreAffairesGlobal(BigDecimal.valueOf(100000))
                .build();

        when(kpiPlateformeService.calculerKpisPlateforme()).thenReturn(dto);

        ResponseEntity<APIResponse<KpiPlateformeDTOResponse>> response =
                adminSystemeDashboardController.getKpiPlateforme();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(5, response.getBody().getData().getNombreSalonsTotal());
    }
}
