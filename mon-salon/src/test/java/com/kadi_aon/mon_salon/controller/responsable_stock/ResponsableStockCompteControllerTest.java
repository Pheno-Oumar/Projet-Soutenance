package com.kadi_aon.mon_salon.controller.responsable_stock;

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
class ResponsableStockCompteControllerTest {

    @Mock
    private CompteService compteService;

    @InjectMocks
    private ResponsableStockCompteController controller;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        org.mockito.Mockito.lenient().when(principal.getName()).thenReturn("stock@test.com");
    }

    @Test
    void getCompte_succes() {
        CompteDTOResponse dto = new CompteDTOResponse(
                1L, "Diop", "Moussa", LocalDate.of(1990, 5, 10), "stock@test.com", "771112233",
                true, "RESPONSABLE_STOCK", null
        );
        when(compteService.getProfil("stock@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<CompteDTOResponse>> response = controller.getCompte(principal);

        assertNotNull(response.getBody());
        assertEquals("Moussa", response.getBody().getData().prenom());
        verify(compteService).getProfil("stock@test.com");
    }

    @Test
    void updateCompte_succes() {
        CompteUpdateDTORequest request = new CompteUpdateDTORequest("Diop", "Moussa", LocalDate.of(1990, 5, 10), "771112244");
        CompteDTOResponse dto = new CompteDTOResponse(
                1L, "Diop", "Moussa", LocalDate.of(1990, 5, 10), "stock@test.com", "771112244",
                true, "RESPONSABLE_STOCK", null
        );
        when(compteService.updateProfil("stock@test.com", request, "mon-salon", "RESPONSABLE_STOCK")).thenReturn(dto);

        ResponseEntity<APIResponse<CompteDTOResponse>> response = controller.updateCompte("mon-salon", request, principal);

        assertNotNull(response.getBody());
        assertEquals("771112244", response.getBody().getData().telephone());
        verify(compteService).updateProfil("stock@test.com", request, "mon-salon", "RESPONSABLE_STOCK");
    }

    @Test
    void changerMotDePasse_succes() {
        ChangementMotDePasseDTORequest request = new ChangementMotDePasseDTORequest("OldPass123!", "NewPass123!");

        ResponseEntity<APIResponse<Void>> response = controller.changerMotDePasse("mon-salon", request, principal);

        assertNotNull(response.getBody());
        verify(compteService).changerMotDePasse("stock@test.com", request, "mon-salon", "RESPONSABLE_STOCK");
    }
}
