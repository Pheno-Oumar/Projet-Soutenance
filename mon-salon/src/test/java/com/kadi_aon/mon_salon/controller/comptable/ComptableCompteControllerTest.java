package com.kadi_aon.mon_salon.controller.comptable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDate;

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
class ComptableCompteControllerTest {

    @Mock
    private CompteService compteService;

    @InjectMocks
    private ComptableCompteController comptableCompteController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("comptable@test.com");
    }

    @Test
    void getCompte_succes() {
        CompteDTOResponse dto = new CompteDTOResponse(
                1L, "Dupont", "Jean", LocalDate.of(1985, 5, 20), "comptable@test.com", "771234567",
                true, "COMPTABLE", null
        );

        when(compteService.getProfil("comptable@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<CompteDTOResponse>> response = comptableCompteController.getCompte(principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Dupont", response.getBody().getData().nom());
        verify(compteService).getProfil("comptable@test.com");
    }

    @Test
    void updateCompte_succes() {
        CompteUpdateDTORequest request = new CompteUpdateDTORequest(
                "NouveauNom", "NouveauPrenom", LocalDate.of(1985, 5, 20), "779998877"
        );
        CompteDTOResponse dto = new CompteDTOResponse(
                1L, "NouveauNom", "NouveauPrenom", LocalDate.of(1985, 5, 20), "comptable@test.com", "779998877",
                true, "COMPTABLE", null
        );

        when(compteService.updateProfil("comptable@test.com", request, "mon-salon", "COMPTABLE")).thenReturn(dto);

        ResponseEntity<APIResponse<CompteDTOResponse>> response =
                comptableCompteController.updateCompte("mon-salon", request, principal);

        assertNotNull(response);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("NouveauNom", response.getBody().getData().nom());
        verify(compteService).updateProfil("comptable@test.com", request, "mon-salon", "COMPTABLE");
    }

    @Test
    void changerMotDePasse_succes() {
        ChangementMotDePasseDTORequest request = new ChangementMotDePasseDTORequest(
                "ancienMdp123", "NouveauMdp123!"
        );

        ResponseEntity<APIResponse<Void>> response =
                comptableCompteController.changerMotDePasse("mon-salon", request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(compteService).changerMotDePasse("comptable@test.com", request, "mon-salon", "COMPTABLE");
    }
}
