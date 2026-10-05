package com.kadi_aon.mon_salon.controller.manager;

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
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationDTOResponse;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationTraiterDTORequest;
import com.kadi_aon.mon_salon.reclamation.enums.StatutReclamation;
import com.kadi_aon.mon_salon.reclamation.service.ReclamationSalonService;

@ExtendWith(MockitoExtension.class)
class ManagerReclamationControllerTest {

    @Mock
    private ReclamationSalonService reclamationSalonService;

    @InjectMocks
    private ManagerReclamationController managerReclamationController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("manager@test.com");
    }

    @Test
    void listerReclamations_succes() {
        ReclamationDTOResponse dto = new ReclamationDTOResponse(
                1L, "Prestation ratée", "Couleur non conforme", "EN_ATTENTE", null,
                LocalDateTime.now(), null, "Client Test", "client@test.com", null, "mon-salon"
        );

        when(reclamationSalonService.listerReclamationsSalon("mon-salon", null)).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<ReclamationDTOResponse>>> resp =
                managerReclamationController.listerReclamations("mon-salon", null);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void obtenirReclamation_succes() {
        ReclamationDTOResponse dto = new ReclamationDTOResponse(
                1L, "Prestation ratée", "Couleur non conforme", "EN_ATTENTE", null,
                LocalDateTime.now(), null, "Client Test", "client@test.com", null, "mon-salon"
        );

        when(reclamationSalonService.obtenirReclamationSalon("mon-salon", 1L)).thenReturn(dto);

        ResponseEntity<APIResponse<ReclamationDTOResponse>> resp =
                managerReclamationController.obtenirReclamation("mon-salon", 1L);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Prestation ratée", resp.getBody().getData().objet());
    }

    @Test
    void traiterReclamation_succes() {
        ReclamationTraiterDTORequest req = new ReclamationTraiterDTORequest(StatutReclamation.RESOLUE, "Remboursement accepté");
        ReclamationDTOResponse dto = new ReclamationDTOResponse(
                1L, "Prestation ratée", "Couleur non conforme", "RESOLUE", "Remboursement accepté",
                LocalDateTime.now(), LocalDateTime.now(), "Client Test", "client@test.com", "Manager Test", "mon-salon"
        );

        when(reclamationSalonService.traiterReclamation("mon-salon", 1L, "manager@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<ReclamationDTOResponse>> resp =
                managerReclamationController.traiterReclamation("mon-salon", 1L, req, principal);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("RESOLUE", resp.getBody().getData().statut());
        verify(reclamationSalonService).traiterReclamation("mon-salon", 1L, "manager@test.com", req);
    }
}
