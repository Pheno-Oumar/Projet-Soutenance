package com.kadi_aon.mon_salon.controller.proprietaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.depense.dto.DepenseDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.FicheClientCompleteDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.KpiSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.PerformanceCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.StockSyntheseDTOResponse;
import com.kadi_aon.mon_salon.salon.service.ProprietairePilotageService;

@ExtendWith(MockitoExtension.class)
class ProprietairePilotageControllerTest {

    @Mock
    private ProprietairePilotageService proprietairePilotageService;

    @Mock
    private com.kadi_aon.mon_salon.facturation.service.RapportFinancierSalonService rapportFinancierSalonService;

    @InjectMocks
    private ProprietairePilotageController proprietairePilotageController;

    @Test
    void getPerformancesCoiffeurs() {
        PerformanceCoiffeurDTOResponse perf = PerformanceCoiffeurDTOResponse.builder()
                .coiffeurId(1L)
                .prenom("Bakary")
                .nombrePrestations(5)
                .chiffreAffaires(BigDecimal.valueOf(25000))
                .build();

        when(proprietairePilotageService.obtenirPerformancesCoiffeurs("salon-chic")).thenReturn(List.of(perf));

        ResponseEntity<APIResponse<List<PerformanceCoiffeurDTOResponse>>> response =
                proprietairePilotageController.getPerformancesCoiffeurs("salon-chic");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Bakary", response.getBody().getData().get(0).getPrenom());
    }

    @Test
    void getKpiSalon() {
        KpiSalonDTOResponse kpi = KpiSalonDTOResponse.builder()
                .slugSalon("salon-chic")
                .nombreClients(20)
                .build();

        when(proprietairePilotageService.obtenirKpiSalon("salon-chic")).thenReturn(kpi);

        ResponseEntity<APIResponse<KpiSalonDTOResponse>> response =
                proprietairePilotageController.getKpiSalon("salon-chic");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(20, response.getBody().getData().getNombreClients());
    }

    @Test
    void getRevenus() {
        PaiementDTOResponse pai = new PaiementDTOResponse(
                1L, "PAI-001", BigDecimal.valueOf(5000), null, "ESPECES", "PAYE", 10L, "FAC-001", 5L, "salon-chic", "Client", "A"
        );

        when(proprietairePilotageService.obtenirRevenusDetailles("salon-chic")).thenReturn(List.of(pai));

        ResponseEntity<APIResponse<List<PaiementDTOResponse>>> response =
                proprietairePilotageController.getRevenus("salon-chic");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void getDepenses() {
        DepenseDTOResponse dep = new DepenseDTOResponse(
                1L, BigDecimal.valueOf(3000), null, "Achat serviettes", "AUTRE", true, "salon-chic", "Comptable", null, null
        );

        when(proprietairePilotageService.obtenirDepensesDetailles("salon-chic")).thenReturn(List.of(dep));

        ResponseEntity<APIResponse<List<DepenseDTOResponse>>> response =
                proprietairePilotageController.getDepenses("salon-chic");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void getStock() {
        StockSyntheseDTOResponse stock = StockSyntheseDTOResponse.builder()
                .produitId(1L)
                .produitNom("Cire")
                .quantiteDisponible(10)
                .build();

        when(proprietairePilotageService.obtenirSyntheseStock("salon-chic")).thenReturn(List.of(stock));

        ResponseEntity<APIResponse<List<StockSyntheseDTOResponse>>> response =
                proprietairePilotageController.getStock("salon-chic");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Cire", response.getBody().getData().get(0).getProduitNom());
    }

    @Test
    void getClients() {
        ClientSalonResumeDTOResponse client = ClientSalonResumeDTOResponse.builder()
                .clientId(5L)
                .nom("Kone")
                .prenom("Fatou")
                .build();

        when(proprietairePilotageService.listerClientsDuSalon("salon-chic")).thenReturn(List.of(client));

        ResponseEntity<APIResponse<List<ClientSalonResumeDTOResponse>>> response =
                proprietairePilotageController.getClients("salon-chic");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Fatou", response.getBody().getData().get(0).getPrenom());
    }

    @Test
    void getFicheClientComplete() {
        FicheClientCompleteDTOResponse fiche = FicheClientCompleteDTOResponse.builder()
                .clientId(5L)
                .nom("Kone")
                .prenom("Fatou")
                .build();

        when(proprietairePilotageService.obtenirFicheClientComplete("salon-chic", 5L)).thenReturn(fiche);

        ResponseEntity<APIResponse<FicheClientCompleteDTOResponse>> response =
                proprietairePilotageController.getFicheClientComplete("salon-chic", 5L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(5L, response.getBody().getData().getClientId());
    }
}
