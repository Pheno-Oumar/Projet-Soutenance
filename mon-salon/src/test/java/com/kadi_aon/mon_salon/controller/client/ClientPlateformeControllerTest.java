package com.kadi_aon.mon_salon.controller.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.favori.service.FavoriSalonService;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.profilcapillaire.dto.CodeProfilDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.service.ProfilCapillaireService;
import com.kadi_aon.mon_salon.reclamation.service.ReclamationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.service.RendezVousService;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTOResponse;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTOResponse;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;
import com.kadi_aon.mon_salon.rgpd.service.RgpdClientService;
import com.kadi_aon.mon_salon.stock.service.CommandeSalonService;
import com.kadi_aon.mon_salon.stock.service.PanierService;

@ExtendWith(MockitoExtension.class)
class ClientPlateformeControllerTest {

    @Mock
    private CompteService compteService;
    @Mock
    private ProfilCapillaireService profilCapillaireService;
    @Mock
    private RendezVousService rendezVousService;
    @Mock
    private PrestationSalonService prestationSalonService;
    @Mock
    private FacturationSalonService facturationSalonService;
    @Mock
    private FavoriSalonService favoriSalonService;
    @Mock
    private AvisSalonService avisSalonService;
    @Mock
    private PanierService panierService;
    @Mock
    private CommandeSalonService commandeSalonService;
    @Mock
    private ReclamationSalonService reclamationSalonService;
    @Mock
    private RgpdClientService rgpdClientService;

    @InjectMocks
    private ClientPlateformeController clientPlateformeController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("client@test.com");
    }

    @Test
    void getCompte_succes() {
        CompteDTOResponse dto = new CompteDTOResponse(
                1L, "Nom", "Prenom", LocalDate.of(1995, 5, 20), "client@test.com", "70000000", true, null, LocalDateTime.now()
        );
        when(compteService.getProfil("client@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<CompteDTOResponse>> response = clientPlateformeController.getCompte(principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("client@test.com", response.getBody().getData().email());
    }

    @Test
    void changerMotDePasse_succes() {
        ChangementMotDePasseDTORequest request = new ChangementMotDePasseDTORequest("oldPass", "newPass");

        ResponseEntity<APIResponse<Void>> response = clientPlateformeController.changerMotDePasse(request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(compteService).changerMotDePasse("client@test.com", request, null, "CLIENT");
    }

    @Test
    void getMonProfilCapillaire_succes() {
        ProfilCapillaireDTOResponse dto = new ProfilCapillaireDTOResponse(
                1L, 10L, "Nom", "Prenom", "Crépu", "Epais", null, null, null, null, null, null, null, true, null, null
        );
        when(profilCapillaireService.getMonProfil("client@test.com")).thenReturn(dto);

        ResponseEntity<APIResponse<ProfilCapillaireDTOResponse>> response =
                clientPlateformeController.getMonProfilCapillaire(principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Crépu", response.getBody().getData().typeCheveux());
    }

    @Test
    void changerCodeProfil_succes() {
        CodeProfilDTORequest request = new CodeProfilDTORequest("1234");

        ResponseEntity<APIResponse<Void>> response = clientPlateformeController.changerCodeProfil(request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(profilCapillaireService).definirOuChangerCodePin("client@test.com", request);
    }

    @Test
    void listerMesRendezVous_transversal() {
        RendezVousDTOResponse dto = new RendezVousDTOResponse(
                1L, "salon-a", "Salon A", 2L, "Coiff", "Bob", "Nom", "Prenom", "010203", null, null, "CONFIRME", null, null, null
        );
        when(rendezVousService.listerTousMesRendezVous("client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<RendezVousDTOResponse>>> response =
                clientPlateformeController.listerMesRendezVous(null, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void demanderSuppression_succes() {
        DemandeSuppressionDTORequest req = new DemandeSuppressionDTORequest("Motif test");
        DemandeSuppressionDTOResponse dto = new DemandeSuppressionDTOResponse(
                1L, 10L, "client@test.com", "Motif test", "EN_ATTENTE", LocalDateTime.now(), null, null, null
        );
        when(rgpdClientService.demanderSuppressionCompte("client@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<DemandeSuppressionDTOResponse>> resp =
                clientPlateformeController.demanderSuppression(req, principal);

        assertNotNull(resp);
        assertEquals(201, resp.getStatusCode().value());
        assertEquals("EN_ATTENTE", resp.getBody().getData().statut());
        verify(rgpdClientService).demanderSuppressionCompte("client@test.com", req);
    }

    @Test
    void consulterSuiviSuppression_succes() {
        DemandeSuppressionDTOResponse dto = new DemandeSuppressionDTOResponse(
                1L, 10L, "client@test.com", "Motif test", "EN_ATTENTE", LocalDateTime.now(), null, null, null
        );
        when(rgpdClientService.consulterMesDemandesSuppression("client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<DemandeSuppressionDTOResponse>>> resp =
                clientPlateformeController.consulterSuiviSuppression(principal);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void demanderExport_succes() {
        DemandeExportDTORequest req = new DemandeExportDTORequest(FormatExportDonnees.JSON);
        DemandeExportDTOResponse dto = new DemandeExportDTOResponse(
                2L, "client@test.com", "JSON", "DISPONIBLE", "https://cloudinary.com/data.json",
                LocalDateTime.now(), LocalDateTime.now().plusHours(48), false
        );
        when(rgpdClientService.demanderExportDonnees("client@test.com", req)).thenReturn(dto);

        ResponseEntity<APIResponse<DemandeExportDTOResponse>> resp =
                clientPlateformeController.demanderExport(req, principal);

        assertEquals(201, resp.getStatusCode().value());
        assertEquals("DISPONIBLE", resp.getBody().getData().statut());
        verify(rgpdClientService).demanderExportDonnees("client@test.com", req);
    }

    @Test
    void consulterArchives_succes() {
        DemandeExportDTOResponse dto = new DemandeExportDTOResponse(
                2L, "client@test.com", "JSON", "DISPONIBLE", "https://cloudinary.com/data.json",
                LocalDateTime.now(), LocalDateTime.now().plusHours(48), false
        );
        when(rgpdClientService.consulterMesExports("client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<DemandeExportDTOResponse>>> resp =
                clientPlateformeController.consulterArchives(principal);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void listerMesCoiffeursFavoris_succes() {
        com.kadi_aon.mon_salon.favori.dto.FavoriCoiffeurDTOResponse dto = new com.kadi_aon.mon_salon.favori.dto.FavoriCoiffeurDTOResponse(
                1L, 10L, "Coiffeur Pro", "photo.png", "mon-salon", "Mon Salon", 100L, LocalDateTime.now()
        );
        when(favoriSalonService.listerTousMesCoiffeursFavoris("client@test.com")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.favori.dto.FavoriCoiffeurDTOResponse>>> resp =
                clientPlateformeController.listerMesCoiffeursFavoris(principal);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
        assertEquals("Coiffeur Pro", resp.getBody().getData().get(0).coiffeurNomComplet());
    }
}
