package com.kadi_aon.mon_salon.controller.responsable_stock;

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
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.KpiStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitAlerteStockDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.RejetCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.dto.RetraitCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.enums.StatutCommande;
import com.kadi_aon.mon_salon.stock.service.CommandeSalonService;

@ExtendWith(MockitoExtension.class)
class ResponsableStockCommandeControllerTest {

    @Mock
    private CommandeSalonService commandeSalonService;

    @InjectMocks
    private ResponsableStockCommandeController controller;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        org.mockito.Mockito.lenient().when(principal.getName()).thenReturn("stock@test.com");
    }

    @Test
    void getProduitsBientotEnRupture_succes() {
        ProduitAlerteStockDTOResponse alerte = new ProduitAlerteStockDTOResponse(
                1L, "Gel", 2L, "Coiffage", new BigDecimal("3000.00"), 2, 5, 20, false
        );
        when(commandeSalonService.obtenirProduitsBientotEnRupture("mon-salon", "stock@test.com"))
                .thenReturn(List.of(alerte));

        ResponseEntity<APIResponse<List<ProduitAlerteStockDTOResponse>>> response =
                controller.getProduitsBientotEnRupture("mon-salon", principal);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void listerCommandes_succes() {
        CommandeDTOResponse cmd = new CommandeDTOResponse(
                10L, "CMD-001", LocalDateTime.now(), "EN_ATTENTE", new BigDecimal("5000.00"),
                "Awa", "awa@test.com", null, null, null, null, null, null, List.of()
        );
        when(commandeSalonService.listerCommandesSalon("mon-salon", "stock@test.com", StatutCommande.EN_ATTENTE, null, null))
                .thenReturn(List.of(cmd));

        ResponseEntity<APIResponse<List<CommandeDTOResponse>>> response =
                controller.listerCommandes("mon-salon", StatutCommande.EN_ATTENTE, null, null, principal);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void validerCommande_succes() {
        CommandeDTOResponse cmd = new CommandeDTOResponse(
                10L, "CMD-001", LocalDateTime.now(), "VALIDEE", new BigDecimal("5000.00"),
                "Awa", "awa@test.com", "RET-123456", null, LocalDateTime.now(), "Moussa", 50L, "FAC-001", List.of()
        );
        when(commandeSalonService.validerCommande("mon-salon", "stock@test.com", 10L)).thenReturn(cmd);

        ResponseEntity<APIResponse<CommandeDTOResponse>> response = controller.validerCommande("mon-salon", 10L, principal);

        assertNotNull(response.getBody());
        assertEquals("VALIDEE", response.getBody().getData().statut());
    }

    @Test
    void rejeterCommande_succes() {
        RejetCommandeDTORequest request = new RejetCommandeDTORequest("Rupture totale inopinée");
        CommandeDTOResponse cmd = new CommandeDTOResponse(
                10L, "CMD-001", LocalDateTime.now(), "REJETEE", new BigDecimal("5000.00"),
                "Awa", "awa@test.com", null, "Rupture totale inopinée", LocalDateTime.now(), "Moussa", null, null, List.of()
        );
        when(commandeSalonService.rejeterCommande("mon-salon", "stock@test.com", 10L, request)).thenReturn(cmd);

        ResponseEntity<APIResponse<CommandeDTOResponse>> response = controller.rejeterCommande("mon-salon", 10L, request, principal);

        assertNotNull(response.getBody());
        assertEquals("REJETEE", response.getBody().getData().statut());
    }

    @Test
    void confirmerRetrait_succes() {
        RetraitCommandeDTORequest request = new RetraitCommandeDTORequest("RET-123456");
        CommandeDTOResponse cmd = new CommandeDTOResponse(
                10L, "CMD-001", LocalDateTime.now(), "RECUPEREE", new BigDecimal("5000.00"),
                "Awa", "awa@test.com", "RET-123456", null, LocalDateTime.now(), "Moussa", 50L, "FAC-001", List.of()
        );
        when(commandeSalonService.confirmerRetrait("mon-salon", "stock@test.com", 10L, request)).thenReturn(cmd);

        ResponseEntity<APIResponse<CommandeDTOResponse>> response = controller.confirmerRetrait("mon-salon", 10L, request, principal);

        assertNotNull(response.getBody());
        assertEquals("RECUPEREE", response.getBody().getData().statut());
    }

    @Test
    void getKpiStock_succes() {
        KpiStockDTOResponse kpi = new KpiStockDTOResponse(
                10, 2, 0, new BigDecimal("100000.00"), 5, 1, 3, 0, 1, new BigDecimal("30000.00")
        );
        when(commandeSalonService.obtenirKpiStock("mon-salon", "stock@test.com")).thenReturn(kpi);

        ResponseEntity<APIResponse<KpiStockDTOResponse>> response = controller.getKpiStock("mon-salon", principal);

        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getData().totalProduitsActifs());
    }
}
