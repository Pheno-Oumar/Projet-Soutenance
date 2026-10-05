package com.kadi_aon.mon_salon.controller.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteServiceDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.VarianteUpdateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.RealisationCreateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.RealisationUpdateDTORequest;
import com.kadi_aon.mon_salon.realisation.service.RealisationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.FicheClientManagerDTOResponse;
import com.kadi_aon.mon_salon.salon.service.ProprietairePilotageService;
import com.kadi_aon.mon_salon.salon.service.ReceptionnisteSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;

@ExtendWith(MockitoExtension.class)
class ManagerControllerTest {

    @Mock
    private CompteService compteService;
    @Mock
    private ServiceSalonService serviceSalonService;
    @Mock
    private AvisSalonService avisSalonService;
    @Mock
    private RealisationSalonService realisationSalonService;
    @Mock
    private ProprietairePilotageService proprietairePilotageService;
    @Mock
    private ReceptionnisteSalonService receptionnisteSalonService;

    @InjectMocks
    private ManagerController managerController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
    }

    @Test
    void listerClients_succes() {
        ClientSalonResumeDTOResponse client = ClientSalonResumeDTOResponse.builder()
                .clientId(1L)
                .nom("Traore")
                .prenom("Fatou")
                .email("fatou@test.com")
                .telephone("771234567")
                .dateNaissance(LocalDate.of(1995, 4, 10))
                .build();

        when(proprietairePilotageService.listerClientsDuSalon("mon-salon")).thenReturn(List.of(client));

        ResponseEntity<APIResponse<List<ClientSalonResumeDTOResponse>>> response =
                managerController.listerClients("mon-salon");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Traore", response.getBody().getData().get(0).getNom());
        verify(proprietairePilotageService).listerClientsDuSalon("mon-salon");
    }

    @Test
    void getClientDetail_succes_sansPlanFinancier() {
        FicheClientManagerDTOResponse fiche = FicheClientManagerDTOResponse.builder()
                .clientId(1L)
                .nom("Traore")
                .prenom("Fatou")
                .email("fatou@test.com")
                .telephone("771234567")
                .dateNaissance(LocalDate.of(1995, 4, 10))
                .rendezVous(List.of())
                .prestations(List.of())
                .build();

        when(proprietairePilotageService.obtenirFicheClientPourManager("mon-salon", 1L)).thenReturn(fiche);

        ResponseEntity<APIResponse<FicheClientManagerDTOResponse>> response =
                managerController.getClientDetail("mon-salon", 1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Fatou", response.getBody().getData().getPrenom());
        verify(proprietairePilotageService).obtenirFicheClientPourManager("mon-salon", 1L);
    }

    @Test
    void getPlanning_succes() {
        LocalDate date = LocalDate.now();
        PlanningRendezVousDTOResponse p = new PlanningRendezVousDTOResponse(
                1L, 5L, "Coiff", "Bob", java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusMinutes(45),
                "Client", "Test", "771234567", 10L, "CONFIRME",
                new BigDecimal("5000.00"), List.of()
        );

        when(receptionnisteSalonService.consulterPlanning("mon-salon", date, 5L)).thenReturn(List.of(p));

        ResponseEntity<APIResponse<List<PlanningRendezVousDTOResponse>>> response =
                managerController.getPlanning("mon-salon", date, 5L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        verify(receptionnisteSalonService).consulterPlanning("mon-salon", date, 5L);
    }

    @Test
    void modifierService_succes() {
        ServiceSalonUpdateDTORequest req = new ServiceSalonUpdateDTORequest("Coupe VIP", "desc");
        ServiceSalonDTOResponse dto = new ServiceSalonDTOResponse(
                1L, "Coupe VIP", "desc", "https://img.url", true, List.of(), null
        );

        when(principal.getName()).thenReturn("manager@test.com");
        when(serviceSalonService.modifierService("mon-salon", 1L, req, "manager@test.com", "MANAGER"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<ServiceSalonDTOResponse>> response =
                managerController.modifierService("mon-salon", 1L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://img.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).modifierService("mon-salon", 1L, req, "manager@test.com", "MANAGER");
    }

    @Test
    void uploadImageService_succes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        ServiceSalonDTOResponse dto = new ServiceSalonDTOResponse(
                1L, "Coupe VIP", "desc", "https://patch.url", true, List.of(), null
        );

        when(principal.getName()).thenReturn("manager@test.com");
        when(serviceSalonService.uploadImageService("mon-salon", 1L, file, "manager@test.com", "MANAGER"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<ServiceSalonDTOResponse>> response =
                managerController.uploadImageService("mon-salon", 1L, file, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://patch.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).uploadImageService("mon-salon", 1L, file, "manager@test.com", "MANAGER");
    }

    @Test
    void ajouterVariante_succes() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        VarianteCreateDTORequest req = new VarianteCreateDTORequest("Chignon", 40, new BigDecimal("35.00"));
        VarianteServiceDTOResponse dto = new VarianteServiceDTOResponse(
                10L, "Chignon", 40, new BigDecimal("35.00"), "https://var.url", true
        );

        when(principal.getName()).thenReturn("manager@test.com");
        when(serviceSalonService.ajouterVariante("mon-salon", 1L, req, image, "manager@test.com", "MANAGER"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<VarianteServiceDTOResponse>> response =
                managerController.ajouterVariante("mon-salon", 1L, req, image, principal);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("https://var.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).ajouterVariante("mon-salon", 1L, req, image, "manager@test.com", "MANAGER");
    }

    @Test
    void modifierVariante_succes() {
        VarianteUpdateDTORequest req = new VarianteUpdateDTORequest("Chignon Mariée", 50, new BigDecimal("45.00"));
        VarianteServiceDTOResponse dto = new VarianteServiceDTOResponse(
                10L, "Chignon Mariée", 50, new BigDecimal("45.00"), "https://newvar.url", true
        );

        when(principal.getName()).thenReturn("manager@test.com");
        when(serviceSalonService.modifierVariante("mon-salon", 1L, 10L, req, "manager@test.com", "MANAGER"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<VarianteServiceDTOResponse>> response =
                managerController.modifierVariante("mon-salon", 1L, 10L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://newvar.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).modifierVariante("mon-salon", 1L, 10L, req, "manager@test.com", "MANAGER");
    }

    @Test
    void uploadImageVariante_succes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        VarianteServiceDTOResponse dto = new VarianteServiceDTOResponse(
                10L, "Chignon", 40, new BigDecimal("35.00"), "https://patchvar.url", true
        );

        when(principal.getName()).thenReturn("manager@test.com");
        when(serviceSalonService.uploadImageVariante("mon-salon", 1L, 10L, file, "manager@test.com", "MANAGER"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<VarianteServiceDTOResponse>> response =
                managerController.uploadImageVariante("mon-salon", 1L, 10L, file, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://patchvar.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).uploadImageVariante("mon-salon", 1L, 10L, file, "manager@test.com", "MANAGER");
    }

    @Test
    void creerRealisation_succes() throws IOException {
        MultipartFile video = mock(MultipartFile.class);
        RealisationCreateDTORequest req = new RealisationCreateDTORequest(
                "Coiffure Tresses", "Magnifiques tresses", 2L, LocalDate.now(), true
        );
        RealisationDTOResponse dto = mock(RealisationDTOResponse.class);
        when(dto.urlVideo()).thenReturn("https://video.url");

        when(principal.getName()).thenReturn("manager@test.com");
        when(realisationSalonService.creerRealisation("mon-salon", "manager@test.com", req, video))
                .thenReturn(dto);

        ResponseEntity<APIResponse<RealisationDTOResponse>> response =
                managerController.creerRealisation("mon-salon", req, video, principal);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("https://video.url", response.getBody().getData().urlVideo());
        verify(realisationSalonService).creerRealisation("mon-salon", "manager@test.com", req, video);
    }

    @Test
    void uploadVideoRealisation_succes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        RealisationDTOResponse dto = mock(RealisationDTOResponse.class);
        when(dto.urlVideo()).thenReturn("https://newvideo.url");

        when(principal.getName()).thenReturn("manager@test.com");
        when(realisationSalonService.uploadVideoRealisation("mon-salon", 1L, "manager@test.com", file))
                .thenReturn(dto);

        ResponseEntity<APIResponse<RealisationDTOResponse>> response =
                managerController.uploadVideoRealisation("mon-salon", 1L, file, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://newvideo.url", response.getBody().getData().urlVideo());
        verify(realisationSalonService).uploadVideoRealisation("mon-salon", 1L, "manager@test.com", file);
    }
}
