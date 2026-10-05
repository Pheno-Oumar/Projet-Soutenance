package com.kadi_aon.mon_salon.controller.admin_systeme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;

@ExtendWith(MockitoExtension.class)
class AdminSystemeCompteControllerTest {

    @Mock
    private CompteService compteService;

    @InjectMocks
    private AdminSystemeCompteController adminSystemeCompteController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("admin@test.com");
    }

    @Test
    void getProfil() {
        CompteDTOResponse dto = new CompteDTOResponse(
                1L, "Traore", "Amadou", null, "admin@test.com", "77112233", true, "ADMIN_SYSTEME", LocalDateTime.now()
        );

        when(compteService.getProfil("admin@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<CompteDTOResponse>> response =
                adminSystemeCompteController.getProfil(principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("admin@test.com", response.getBody().getData().email());
    }

    @Test
    void updateProfil() {
        CompteUpdateDTORequest req = new CompteUpdateDTORequest("Traore", "Amadou", null, "77112233");
        CompteDTOResponse dto = new CompteDTOResponse(
                1L, "Traore", "Amadou", null, "admin@test.com", "77112233", true, "ADMIN_SYSTEME", LocalDateTime.now()
        );

        when(compteService.updateProfil("admin@test.com", req, null, "ADMIN_SYSTEME")).thenReturn(dto);

        ResponseEntity<APIResponse<CompteDTOResponse>> response =
                adminSystemeCompteController.updateProfil(req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("77112233", response.getBody().getData().telephone());
    }

    @Test
    void changerMotDePasse() {
        ChangementMotDePasseDTORequest req = new ChangementMotDePasseDTORequest("OldPass123!", "NewPass123!");

        ResponseEntity<APIResponse<Void>> response =
                adminSystemeCompteController.changerMotDePasse(req, principal);

        verify(compteService).changerMotDePasse("admin@test.com", req, null, "ADMIN_SYSTEME");
        assertEquals(200, response.getStatusCode().value());
    }
}
