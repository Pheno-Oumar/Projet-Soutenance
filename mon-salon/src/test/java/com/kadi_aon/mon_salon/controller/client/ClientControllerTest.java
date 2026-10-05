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
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.favori.dto.FavoriSalonDTOResponse;
import com.kadi_aon.mon_salon.favori.service.FavoriSalonService;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousCreateDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.service.RendezVousService;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private RendezVousService rendezVousService;
    @Mock
    private PrestationSalonService prestationSalonService;
    @Mock
    private FacturationSalonService facturationSalonService;
    @Mock
    private AvisSalonService avisSalonService;
    @Mock
    private FavoriSalonService favoriSalonService;

    @InjectMocks
    private ClientController clientController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("client@test.com");
    }

    @Test
    void reserverRendezVous_succes() {
        LocalDateTime datePrevue = LocalDateTime.now().plusDays(1);
        RendezVousCreateDTORequest req = new RendezVousCreateDTORequest(datePrevue, List.of(1L), 2L);
        RendezVousDTOResponse dto = new RendezVousDTOResponse(
                1L, "mon-salon", "Mon Salon", 2L, "Coiff", "Bob", "Nom", "Prenom", "010203", datePrevue, null, "CONFIRME", null, null, null
        );
        when(rendezVousService.creerRendezVousClient("mon-salon", req, "client@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<RendezVousDTOResponse>> response =
                clientController.reserverRendezVous("mon-salon", req, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("CONFIRME", response.getBody().getData().statut());
    }

    @Test
    void annulerRendezVous_succes() {
        RendezVousAnnulationDTORequest request = new RendezVousAnnulationDTORequest("Imprévu");
        RendezVousDTOResponse dto = new RendezVousDTOResponse(
                1L, "mon-salon", "Salon", 2L, "Coiff", "Bob", "Nom", "Prenom", "010203", null, null, "ANNULE", null, null, null
        );
        when(rendezVousService.annulerRendezVousClient("mon-salon", 1L, request, "client@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<RendezVousDTOResponse>> response =
                clientController.annulerRendezVous("mon-salon", 1L, request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("ANNULE", response.getBody().getData().statut());
    }

    @Test
    void listerMesRendezVous_succes() {
        RendezVousDTOResponse dto = new RendezVousDTOResponse(
                1L, "mon-salon", "Salon", 2L, "Coiff", "Bob", "Nom", "Prenom", "010203", null, null, "CONFIRME", null, null, null
        );
        when(rendezVousService.listerMesRendezVous("mon-salon", "client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<RendezVousDTOResponse>>> response =
                clientController.listerMesRendezVous("mon-salon", principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void listerMesPrestations_succes() {
        PrestationDTOResponse dto = new PrestationDTOResponse(
                1L, "mon-salon", 2L, "Coiff", "Bob", 10L, "Nom", "Prenom", "010203", 2L,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                BigDecimal.valueOf(5000), "TERMINE", null, List.of()
        );
        when(prestationSalonService.listerPrestationsClientSalon("mon-salon", "client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<PrestationDTOResponse>>> response =
                clientController.listerMesPrestations("mon-salon", principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void listerMesPaiements_succes() {
        PaiementDTOResponse dto = new PaiementDTOResponse(
                1L, "PAY-001", BigDecimal.valueOf(5000), LocalDateTime.now(), "PRESTATION", "EFFECTUE",
                1L, "FAC-001", 1L, "mon-salon", "Nom", "Prenom"
        );
        when(facturationSalonService.listerPaiementsClient("mon-salon", "client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<PaiementDTOResponse>>> response =
                clientController.listerMesPaiements("mon-salon", principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void ajouterSalonFavori_succes() {
        FavoriSalonDTOResponse dto = new FavoriSalonDTOResponse(1L, "mon-salon", "Mon Salon", "logo.png", 10L, LocalDateTime.now());
        when(favoriSalonService.ajouterSalonFavori("mon-salon", "client@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<FavoriSalonDTOResponse>> response =
                clientController.ajouterSalonFavori("mon-salon", principal);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("mon-salon", response.getBody().getData().salonSlug());
    }

    @Test
    void retirerSalonFavori_succes() {
        ResponseEntity<APIResponse<Void>> response =
                clientController.retirerSalonFavori("mon-salon", principal);

        assertEquals(200, response.getStatusCode().value());
        verify(favoriSalonService).retirerSalonFavori("mon-salon", "client@test.com");
    }

    @Test
    void supprimerAvisPrestation_succes() {
        ResponseEntity<APIResponse<Void>> response =
                clientController.supprimerAvisPrestation("mon-salon", 42L, principal);

        assertEquals(200, response.getStatusCode().value());
        verify(avisSalonService).supprimerAvisPrestation("mon-salon", 42L, "client@test.com");
    }

    @Test
    void supprimerAvisSalon_succes() {
        ResponseEntity<APIResponse<Void>> response =
                clientController.supprimerAvisSalon("mon-salon", principal);

        assertEquals(200, response.getStatusCode().value());
        verify(avisSalonService).supprimerAvisSalon("mon-salon", "client@test.com");
    }
}
