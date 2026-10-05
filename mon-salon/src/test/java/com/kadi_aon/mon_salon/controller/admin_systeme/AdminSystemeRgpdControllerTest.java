package com.kadi_aon.mon_salon.controller.admin_systeme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDecisionDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTOResponse;
import com.kadi_aon.mon_salon.rgpd.service.RgpdAdminService;

@ExtendWith(MockitoExtension.class)
class AdminSystemeRgpdControllerTest {

    @Mock
    private RgpdAdminService rgpdAdminService;

    @InjectMocks
    private AdminSystemeRgpdController adminSystemeRgpdController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("admin@test.com");
    }

    @Test
    void listerDemandesSuppression_succes() {
        DemandeSuppressionDTOResponse dto = new DemandeSuppressionDTOResponse(
                1L, 10L, "client@test.com", "Motif", "EN_ATTENTE", LocalDateTime.now(), null, null, null
        );

        when(rgpdAdminService.listerDemandesSuppression(null)).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<DemandeSuppressionDTOResponse>>> resp =
                adminSystemeRgpdController.listerDemandesSuppression(null);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void obtenirDemandeSuppression_succes() {
        DemandeSuppressionDTOResponse dto = new DemandeSuppressionDTOResponse(
                1L, 10L, "client@test.com", "Motif", "EN_ATTENTE", LocalDateTime.now(), null, null, null
        );

        when(rgpdAdminService.obtenirDemandeSuppression(1L)).thenReturn(dto);

        ResponseEntity<APIResponse<DemandeSuppressionDTOResponse>> resp =
                adminSystemeRgpdController.obtenirDemandeSuppression(1L);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1L, resp.getBody().getData().id());
    }

    @Test
    void traiterDemandeSuppression_succes() {
        DemandeSuppressionDecisionDTORequest req = new DemandeSuppressionDecisionDTORequest(true, "Approuvé");
        DemandeSuppressionDTOResponse dto = new DemandeSuppressionDTOResponse(
                1L, 10L, "client@test.com", "Motif", "APPROUVEE", LocalDateTime.now(), LocalDateTime.now(), "Approuvé", "admin@test.com"
        );

        when(rgpdAdminService.traiterDemandeSuppression(1L, "admin@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<DemandeSuppressionDTOResponse>> resp =
                adminSystemeRgpdController.traiterDemandeSuppression(1L, req, principal);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("APPROUVEE", resp.getBody().getData().statut());
        verify(rgpdAdminService).traiterDemandeSuppression(1L, "admin@test.com", req);
    }
}
