package com.kadi_aon.mon_salon.controller.comptable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseClotureDTORequest;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseDTOResponse;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseOuvertureDTORequest;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;

@ExtendWith(MockitoExtension.class)
class ComptableCaisseControllerTest {

    @Mock
    private CaisseSalonService caisseSalonService;

    @InjectMocks
    private ComptableCaisseController comptableCaisseController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("comptable@test.com");
    }

    @Test
    void ouvrirSession_succes() {
        SessionCaisseOuvertureDTORequest request = new SessionCaisseOuvertureDTORequest(new BigDecimal("100.00"));
        SessionCaisseDTOResponse dto = new SessionCaisseDTOResponse(
                1L, 10L, "Dupont", "Jean", "comptable@test.com",
                LocalDateTime.now(), null, new BigDecimal("100.00"), null,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100.00"), "EN_COURS", List.of()
        );

        when(caisseSalonService.ouvrirSessionCaisse("mon-salon", "comptable@test.com", request)).thenReturn(dto);

        ResponseEntity<APIResponse<SessionCaisseDTOResponse>> response =
                comptableCaisseController.ouvrirSession("mon-salon", request, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("EN_COURS", response.getBody().getData().statut());
        verify(caisseSalonService).ouvrirSessionCaisse("mon-salon", "comptable@test.com", request);
    }

    @Test
    void cloturerSession_succes() {
        SessionCaisseClotureDTORequest request = new SessionCaisseClotureDTORequest(new BigDecimal("350.00"));
        SessionCaisseDTOResponse dto = new SessionCaisseDTOResponse(
                1L, 10L, "Dupont", "Jean", "comptable@test.com",
                LocalDateTime.now().minusHours(8), LocalDateTime.now(),
                new BigDecimal("100.00"), new BigDecimal("350.00"),
                new BigDecimal("250.00"), BigDecimal.ZERO, new BigDecimal("350.00"), "CLOTURE", List.of()
        );

        when(caisseSalonService.cloturerSessionCaisse("mon-salon", "comptable@test.com", request)).thenReturn(dto);

        ResponseEntity<APIResponse<SessionCaisseDTOResponse>> response =
                comptableCaisseController.cloturerSession("mon-salon", request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("CLOTURE", response.getBody().getData().statut());
        verify(caisseSalonService).cloturerSessionCaisse("mon-salon", "comptable@test.com", request);
    }

    @Test
    void getSessionCourante_succes() {
        SessionCaisseDTOResponse dto = new SessionCaisseDTOResponse(
                1L, 10L, "Dupont", "Jean", "comptable@test.com",
                LocalDateTime.now(), null, new BigDecimal("100.00"), null,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100.00"), "EN_COURS", List.of()
        );

        when(caisseSalonService.obtenirSessionCourante("mon-salon")).thenReturn(dto);

        ResponseEntity<APIResponse<SessionCaisseDTOResponse>> response =
                comptableCaisseController.getSessionCourante("mon-salon");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getData().id());
    }

    @Test
    void getOperationsSession_succes() {
        com.kadi_aon.mon_salon.caisse.dto.OperationCaisseDTOResponse op =
                new com.kadi_aon.mon_salon.caisse.dto.OperationCaisseDTOResponse(
                        10L, new BigDecimal("50.00"), LocalDateTime.now(), "Encaissement", "ENTREE", true, "PAY-001"
                );

        when(caisseSalonService.listerOperationsSession("mon-salon", 1L)).thenReturn(List.of(op));

        ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.caisse.dto.OperationCaisseDTOResponse>>> response =
                comptableCaisseController.getOperationsSession("mon-salon", 1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("PAY-001", response.getBody().getData().get(0).numeroPaiement());
        verify(caisseSalonService).listerOperationsSession("mon-salon", 1L);
    }
}

