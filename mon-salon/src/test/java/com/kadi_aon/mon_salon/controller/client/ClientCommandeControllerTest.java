package com.kadi_aon.mon_salon.controller.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.service.CommandeSalonService;

@ExtendWith(MockitoExtension.class)
class ClientCommandeControllerTest {

    @Mock
    private CommandeSalonService commandeSalonService;

    @InjectMocks
    private ClientCommandeController controller;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        org.mockito.Mockito.lenient().when(principal.getName()).thenReturn("client@test.com");
    }

    @Test
    void passerCommande_succes() {
        CommandeDTOResponse dto = new CommandeDTOResponse(
                1L, "CMD-001", LocalDateTime.now(), "EN_ATTENTE", new BigDecimal("12000.00"),
                "Awa", "client@test.com", null, null, null, null, null, null, List.of()
        );
        when(commandeSalonService.passerCommande("mon-salon", "client@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<CommandeDTOResponse>> response = controller.passerCommande("mon-salon", principal);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("CMD-001", response.getBody().getData().numeroCommande());
    }

    @Test
    void listerCommandes_succes() {
        CommandeDTOResponse dto = new CommandeDTOResponse(
                1L, "CMD-001", LocalDateTime.now(), "EN_ATTENTE", new BigDecimal("12000.00"),
                "Awa", "client@test.com", null, null, null, null, null, null, List.of()
        );
        when(commandeSalonService.listerCommandesClient("mon-salon", "client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<CommandeDTOResponse>>> response = controller.listerCommandes("mon-salon", principal);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void obtenirCommande_succes() {
        CommandeDTOResponse dto = new CommandeDTOResponse(
                1L, "CMD-001", LocalDateTime.now(), "EN_ATTENTE", new BigDecimal("12000.00"),
                "Awa", "client@test.com", null, null, null, null, null, null, List.of()
        );
        when(commandeSalonService.obtenirCommandeClient("mon-salon", "client@test.com", 1L)).thenReturn(dto);

        ResponseEntity<APIResponse<CommandeDTOResponse>> response = controller.obtenirCommande("mon-salon", 1L, principal);

        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getData().id());
    }
}
