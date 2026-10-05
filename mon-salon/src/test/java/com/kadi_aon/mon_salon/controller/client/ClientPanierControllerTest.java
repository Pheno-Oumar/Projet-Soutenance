package com.kadi_aon.mon_salon.controller.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.stock.dto.AjoutPanierDTORequest;
import com.kadi_aon.mon_salon.stock.dto.ModificationQuantiteDTORequest;
import com.kadi_aon.mon_salon.stock.dto.PanierDTOResponse;
import com.kadi_aon.mon_salon.stock.service.PanierService;

@ExtendWith(MockitoExtension.class)
class ClientPanierControllerTest {

    @Mock
    private PanierService panierService;

    @InjectMocks
    private ClientPanierController controller;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        org.mockito.Mockito.lenient().when(principal.getName()).thenReturn("client@test.com");
    }

    @Test
    void getPanier_succes() {
        PanierDTOResponse dto = new PanierDTOResponse(
                1L, "mon-salon", "client@test.com", List.of(), BigDecimal.ZERO, 0, LocalDateTime.now()
        );
        when(panierService.obtenirPanier("mon-salon", "client@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<PanierDTOResponse>> response = controller.getPanier("mon-salon", principal);

        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getData().nombreArticles());
    }

    @Test
    void ajouterArticle_succes() {
        AjoutPanierDTORequest request = new AjoutPanierDTORequest(10L, 2);
        PanierDTOResponse dto = new PanierDTOResponse(
                1L, "mon-salon", "client@test.com", List.of(), new BigDecimal("10000.00"), 2, LocalDateTime.now()
        );
        when(panierService.ajouterArticle("mon-salon", "client@test.com", request)).thenReturn(dto);

        ResponseEntity<APIResponse<PanierDTOResponse>> response = controller.ajouterArticle("mon-salon", request, principal);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(2, response.getBody().getData().nombreArticles());
    }

    @Test
    void modifierQuantite_succes() {
        ModificationQuantiteDTORequest request = new ModificationQuantiteDTORequest(5);
        PanierDTOResponse dto = new PanierDTOResponse(
                1L, "mon-salon", "client@test.com", List.of(), new BigDecimal("25000.00"), 5, LocalDateTime.now()
        );
        when(panierService.modifierQuantite("mon-salon", "client@test.com", 10L, request)).thenReturn(dto);

        ResponseEntity<APIResponse<PanierDTOResponse>> response = controller.modifierQuantite("mon-salon", 10L, request, principal);

        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getData().nombreArticles());
    }

    @Test
    void supprimerArticle_succes() {
        PanierDTOResponse dto = new PanierDTOResponse(
                1L, "mon-salon", "client@test.com", List.of(), BigDecimal.ZERO, 0, LocalDateTime.now()
        );
        when(panierService.supprimerArticle("mon-salon", "client@test.com", 10L)).thenReturn(dto);

        ResponseEntity<APIResponse<PanierDTOResponse>> response = controller.supprimerArticle("mon-salon", 10L, principal);

        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getData().nombreArticles());
    }

    @Test
    void viderPanier_succes() {
        ResponseEntity<APIResponse<Void>> response = controller.viderPanier("mon-salon", principal);

        assertNotNull(response.getBody());
        verify(panierService).viderPanier("mon-salon", "client@test.com");
    }
}
