package com.kadi_aon.mon_salon.controller.receptionniste;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse;
//import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
//import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDeplacerDTORequest;
//import com.kadi_aon.mon_salon.rendezvous.dto.RetardRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardTraitementDTORequest;
import com.kadi_aon.mon_salon.rendezvous.enums.ActionTraitementRetard;
import com.kadi_aon.mon_salon.rendezvous.service.DisponibiliteService;
import com.kadi_aon.mon_salon.salon.service.ReceptionnisteSalonService;

@ExtendWith(MockitoExtension.class)
class ReceptionnisteControllerTest {

    @Mock
    private ReceptionnisteSalonService receptionnisteSalonService;
    @Mock
    private CompteService compteService;
    @Mock
    private DisponibiliteService disponibiliteService;
    @Mock
    private com.kadi_aon.mon_salon.prestation.service.PrestationSalonService prestationSalonService;
    @Mock
    private com.kadi_aon.mon_salon.facturation.service.FacturationSalonService facturationSalonService;
    @Mock
    private com.kadi_aon.mon_salon.caisse.service.CaisseSalonService caisseSalonService;
    @Mock
    private com.kadi_aon.mon_salon.rendezvous.service.RendezVousService rendezVousService;

    @InjectMocks
    private ReceptionnisteController receptionnisteController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        org.mockito.Mockito.lenient().when(principal.getName()).thenReturn("recep@test.com");
    }

    @Test
    void changerMotDePasse_appelleCompteService() {
        ChangementMotDePasseDTORequest request = new ChangementMotDePasseDTORequest("ancien", "nouveau");

        ResponseEntity<APIResponse<Void>> response = receptionnisteController.changerMotDePasse("mon-salon", request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(compteService).changerMotDePasse("recep@test.com", request, "mon-salon", "RECEPTIONNISTE");
    }

    @Test
    void consulterPlanning_succes() {
        LocalDate date = LocalDate.now();
        when(receptionnisteSalonService.consulterPlanning("mon-salon", date, null)).thenReturn(List.of());

        ResponseEntity<APIResponse<List<PlanningRendezVousDTOResponse>>> response =
                receptionnisteController.consulterPlanning("mon-salon", date, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void traiterRetard_succes() {
        RetardTraitementDTORequest request = new RetardTraitementDTORequest(ActionTraitementRetard.NO_SHOW, null, "Absent");
        RendezVousDTOResponse dto = new RendezVousDTOResponse(
                1L, "mon-salon", "Salon", 2L, "Coiff", "Bob", "Nom", "Prenom", "010203", null, null, "NO_SHOW", null, null, null
        );
        when(receptionnisteSalonService.traiterRetard("mon-salon", 1L, request, "recep@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<RendezVousDTOResponse>> response =
                receptionnisteController.traiterRetard("mon-salon", 1L, request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("NO_SHOW", response.getBody().getData().statut());
    }

    @Test
    void getDashboard_succes() {
        com.kadi_aon.mon_salon.salon.dto.ReceptionnisteStatsJourDTO stats =
                new com.kadi_aon.mon_salon.salon.dto.ReceptionnisteStatsJourDTO(5, 2, 1, 2, 0, 0);
        com.kadi_aon.mon_salon.salon.dto.ReceptionnisteCaisseStatutDTO caisse =
                new com.kadi_aon.mon_salon.salon.dto.ReceptionnisteCaisseStatutDTO(true, 10L, java.time.LocalDateTime.now(), java.math.BigDecimal.valueOf(50000), java.math.BigDecimal.valueOf(125000), 4);
        com.kadi_aon.mon_salon.salon.dto.ReceptionnisteDashboardDTOResponse dashboardDto =
                new com.kadi_aon.mon_salon.salon.dto.ReceptionnisteDashboardDTOResponse(stats, caisse, List.of(), List.of(), List.of());

        when(receptionnisteSalonService.getDashboard("mon-salon")).thenReturn(dashboardDto);

        ResponseEntity<APIResponse<com.kadi_aon.mon_salon.salon.dto.ReceptionnisteDashboardDTOResponse>> response =
                receptionnisteController.getDashboard("mon-salon");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(5, response.getBody().getData().stats().totalRendezVousJour());
        assertEquals(true, response.getBody().getData().caisse().caisseOuverte());
    }

    @Test
    void annulerRendezVous_succes() {
        com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest request =
                new com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest("Empêchement client");
        RendezVousDTOResponse dto = new RendezVousDTOResponse(
                1L, "mon-salon", "Salon", 2L, "Coiff", "Bob", "Nom", "Prenom", "010203", null, null, "ANNULE", null, null, null
        );

        when(receptionnisteSalonService.annulerRendezVous("mon-salon", 1L, request, "recep@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<RendezVousDTOResponse>> response =
                receptionnisteController.annulerRendezVous("mon-salon", 1L, request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("ANNULE", response.getBody().getData().statut());
    }
}
