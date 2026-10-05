package com.kadi_aon.mon_salon.controller.client;

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
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationCreateDTORequest;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationDTOResponse;
import com.kadi_aon.mon_salon.reclamation.service.ReclamationSalonService;

@ExtendWith(MockitoExtension.class)
class ClientReclamationControllerTest {

    @Mock
    private ReclamationSalonService reclamationSalonService;

    @InjectMocks
    private ClientReclamationController clientReclamationController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("client@test.com");
    }

    @Test
    void deposerReclamation_succes() {
        ReclamationCreateDTORequest req = new ReclamationCreateDTORequest("Retard", "Retard de 20 minutes");
        ReclamationDTOResponse dto = new ReclamationDTOResponse(
                1L, "Retard", "Retard de 20 minutes", "EN_ATTENTE", null,
                LocalDateTime.now(), null, "Client Test", "client@test.com", null, "mon-salon"
        );

        when(reclamationSalonService.deposerReclamation("mon-salon", "client@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<ReclamationDTOResponse>> resp =
                clientReclamationController.deposerReclamation("mon-salon", req, principal);

        assertNotNull(resp);
        assertEquals(201, resp.getStatusCode().value());
        assertEquals("Retard", resp.getBody().getData().objet());
        verify(reclamationSalonService).deposerReclamation("mon-salon", "client@test.com", req);
    }

    @Test
    void listerMesReclamations_succes() {
        ReclamationDTOResponse dto = new ReclamationDTOResponse(
                1L, "Retard", "Retard", "EN_ATTENTE", null,
                LocalDateTime.now(), null, "Client Test", "client@test.com", null, "mon-salon"
        );

        when(reclamationSalonService.listerReclamationsClient("mon-salon", "client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<ReclamationDTOResponse>>> resp =
                clientReclamationController.listerMesReclamations("mon-salon", principal);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void obtenirMaReclamation_succes() {
        ReclamationDTOResponse dto = new ReclamationDTOResponse(
                1L, "Retard", "Retard", "EN_ATTENTE", null,
                LocalDateTime.now(), null, "Client Test", "client@test.com", null, "mon-salon"
        );

        when(reclamationSalonService.obtenirReclamationClient("mon-salon", 1L, "client@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<ReclamationDTOResponse>> resp =
                clientReclamationController.obtenirMaReclamation("mon-salon", 1L, principal);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1L, resp.getBody().getData().id());
    }
}
