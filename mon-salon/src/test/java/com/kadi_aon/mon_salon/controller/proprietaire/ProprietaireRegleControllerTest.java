package com.kadi_aon.mon_salon.controller.proprietaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonCreateDTORequest;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonDTOResponse;
import com.kadi_aon.mon_salon.regle.dto.RegleSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.regle.service.RegleSalonService;

@ExtendWith(MockitoExtension.class)
class ProprietaireRegleControllerTest {

    @Mock
    private RegleSalonService regleSalonService;

    @InjectMocks
    private ProprietaireRegleController proprietaireRegleController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn("proprio@test.com");
    }

    @Test
    void creerRegle() {
        RegleSalonCreateDTORequest req = new RegleSalonCreateDTORequest("Règle 1", "Desc");
        RegleSalonDTOResponse dto = RegleSalonDTOResponse.builder().id(1L).titre("Règle 1").build();

        when(regleSalonService.creerRegle("salon-chic", req, "proprio@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<RegleSalonDTOResponse>> response =
                proprietaireRegleController.creerRegle("salon-chic", req, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("Règle 1", response.getBody().getData().getTitre());
    }

    @Test
    void listerRegles() {
        RegleSalonDTOResponse dto = RegleSalonDTOResponse.builder().id(1L).build();
        when(regleSalonService.listerReglesDuSalon("salon-chic")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<RegleSalonDTOResponse>>> response =
                proprietaireRegleController.listerRegles("salon-chic");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void modifierRegle() {
        RegleSalonUpdateDTORequest req = new RegleSalonUpdateDTORequest("Titre Modifie", "Desc");
        RegleSalonDTOResponse dto = RegleSalonDTOResponse.builder().id(1L).titre("Titre Modifie").build();

        when(regleSalonService.modifierRegle("salon-chic", 1L, req, "proprio@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<RegleSalonDTOResponse>> response =
                proprietaireRegleController.modifierRegle("salon-chic", 1L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Titre Modifie", response.getBody().getData().getTitre());
    }

    @Test
    void supprimerRegle() {
        ResponseEntity<APIResponse<Void>> response =
                proprietaireRegleController.supprimerRegle("salon-chic", 1L, principal);

        verify(regleSalonService).supprimerRegle("salon-chic", 1L, "proprio@test.com");
        assertEquals(200, response.getStatusCode().value());
    }
}
