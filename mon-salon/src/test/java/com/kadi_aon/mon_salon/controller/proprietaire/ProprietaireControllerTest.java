package com.kadi_aon.mon_salon.controller.proprietaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.service.CoiffeurSalonService;
import com.kadi_aon.mon_salon.salon.dto.FermetureExceptionnelleDTORequest;
import com.kadi_aon.mon_salon.salon.dto.FermetureExceptionnelleDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteServiceDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.VarianteUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.service.EmployeSalonService;
import com.kadi_aon.mon_salon.salon.service.HoraireSalonService;
import com.kadi_aon.mon_salon.salon.service.ProprietaireSalonService;
import com.kadi_aon.mon_salon.salon.service.ReceptionnisteSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;

@ExtendWith(MockitoExtension.class)
class ProprietaireControllerTest {

    @Mock
    private ProprietaireSalonService proprietaireSalonService;
    @Mock
    private CompteService compteService;
    @Mock
    private EmployeSalonService employeSalonService;
    @Mock
    private HoraireSalonService horaireSalonService;
    @Mock
    private ServiceSalonService serviceSalonService;
    @Mock
    private CoiffeurSalonService coiffeurSalonService;
    @Mock
    private ReceptionnisteSalonService receptionnisteSalonService;

    @InjectMocks
    private ProprietaireController proprietaireController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
    }

    @Test
    void creerService_succes() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        ServiceSalonCreateDTORequest req = new ServiceSalonCreateDTORequest("Coiffure Moderne", "desc", List.of());
        ServiceSalonDTOResponse dto = new ServiceSalonDTOResponse(
                1L, "Coiffure Moderne", "desc", "https://img.url", true, List.of(), null
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(serviceSalonService.creerService("mon-salon", req, image, "proprio@test.com", "PROPRIETAIRE"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<ServiceSalonDTOResponse>> response =
                proprietaireController.creerService("mon-salon", req, image, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("https://img.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).creerService("mon-salon", req, image, "proprio@test.com", "PROPRIETAIRE");
    }

    @Test
    void modifierService_succes() {
        ServiceSalonUpdateDTORequest req = new ServiceSalonUpdateDTORequest("Coiffure VIP", "nouvelle description");
        ServiceSalonDTOResponse dto = new ServiceSalonDTOResponse(
                1L, "Coiffure VIP", "nouvelle description", "https://img.url", true, List.of(), null
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(serviceSalonService.modifierService("mon-salon", 1L, req, "proprio@test.com", "PROPRIETAIRE"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<ServiceSalonDTOResponse>> response =
                proprietaireController.modifierService("mon-salon", 1L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Coiffure VIP", response.getBody().getData().nom());
        verify(serviceSalonService).modifierService("mon-salon", 1L, req, "proprio@test.com", "PROPRIETAIRE");
    }

    @Test
    void uploadImageService_succes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        ServiceSalonDTOResponse dto = new ServiceSalonDTOResponse(
                1L, "Coiffure VIP", "desc", "https://patch.url", true, List.of(), null
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(serviceSalonService.uploadImageService("mon-salon", 1L, file, "proprio@test.com", "PROPRIETAIRE"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<ServiceSalonDTOResponse>> response =
                proprietaireController.uploadImageService("mon-salon", 1L, file, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://patch.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).uploadImageService("mon-salon", 1L, file, "proprio@test.com", "PROPRIETAIRE");
    }

    @Test
    void ajouterVariante_succes() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        VarianteCreateDTORequest req = new VarianteCreateDTORequest("Chignon", 40, new BigDecimal("35.00"));
        VarianteServiceDTOResponse dto = new VarianteServiceDTOResponse(
                10L, "Chignon", 40, new BigDecimal("35.00"), "https://var.url", true
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(serviceSalonService.ajouterVariante("mon-salon", 1L, req, image, "proprio@test.com", "PROPRIETAIRE"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<VarianteServiceDTOResponse>> response =
                proprietaireController.ajouterVariante("mon-salon", 1L, req, image, principal);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("https://var.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).ajouterVariante("mon-salon", 1L, req, image, "proprio@test.com", "PROPRIETAIRE");
    }

    @Test
    void modifierVariante_succes() {
        VarianteUpdateDTORequest req = new VarianteUpdateDTORequest("Chignon Mariée", 50, new BigDecimal("45.00"));
        VarianteServiceDTOResponse dto = new VarianteServiceDTOResponse(
                10L, "Chignon Mariée", 50, new BigDecimal("45.00"), "https://var.url", true
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(serviceSalonService.modifierVariante("mon-salon", 1L, 10L, req, "proprio@test.com", "PROPRIETAIRE"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<VarianteServiceDTOResponse>> response =
                proprietaireController.modifierVariante("mon-salon", 1L, 10L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Chignon Mariée", response.getBody().getData().nom());
        verify(serviceSalonService).modifierVariante("mon-salon", 1L, 10L, req, "proprio@test.com", "PROPRIETAIRE");
    }

    @Test
    void uploadImageVariante_succes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        VarianteServiceDTOResponse dto = new VarianteServiceDTOResponse(
                10L, "Chignon", 40, new BigDecimal("35.00"), "https://patchvar.url", true
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(serviceSalonService.uploadImageVariante("mon-salon", 1L, 10L, file, "proprio@test.com", "PROPRIETAIRE"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<VarianteServiceDTOResponse>> response =
                proprietaireController.uploadImageVariante("mon-salon", 1L, 10L, file, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://patchvar.url", response.getBody().getData().imageUrl());
        verify(serviceSalonService).uploadImageVariante("mon-salon", 1L, 10L, file, "proprio@test.com", "PROPRIETAIRE");
    }

    @Test
    void modifierFermeture_succes() {
        FermetureExceptionnelleDTORequest req = new FermetureExceptionnelleDTORequest(
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), "Travaux de rénovation"
        );
        FermetureExceptionnelleDTOResponse dto = new FermetureExceptionnelleDTOResponse(
                5L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), "Travaux de rénovation", LocalDateTime.now()
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(horaireSalonService.modifierFermeture("mon-salon", 5L, req, "proprio@test.com"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<FermetureExceptionnelleDTOResponse>> response =
                proprietaireController.modifierFermeture("mon-salon", 5L, req, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Travaux de rénovation", response.getBody().getData().motif());
        verify(horaireSalonService).modifierFermeture("mon-salon", 5L, req, "proprio@test.com");
    }

    @Test
    void mettreFinFermeture_succes() {
        FermetureExceptionnelleDTOResponse dto = new FermetureExceptionnelleDTOResponse(
                5L, LocalDate.now().minusDays(1), LocalDate.now(), "Travaux terminés plus tôt", LocalDateTime.now()
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(horaireSalonService.mettreFinFermeture("mon-salon", 5L, "proprio@test.com"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<FermetureExceptionnelleDTOResponse>> response =
                proprietaireController.mettreFinFermeture("mon-salon", 5L, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(LocalDate.now(), response.getBody().getData().dateFin());
        verify(horaireSalonService).mettreFinFermeture("mon-salon", 5L, "proprio@test.com");
    }

    @Test
    void mettreFinIndisponibilite_succes() {
        IndisponibiliteDTOResponse dto = new IndisponibiliteDTOResponse(
                12L, 2L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now(),
                "AUTRE",
                "Retour anticipé",
                LocalDateTime.now()
        );

        when(principal.getName()).thenReturn("proprio@test.com");
        when(coiffeurSalonService.mettreFinIndisponibiliteParManagerOuProprio("mon-salon", 12L, "proprio@test.com"))
                .thenReturn(dto);

        ResponseEntity<APIResponse<IndisponibiliteDTOResponse>> response =
                proprietaireController.mettreFinIndisponibilite("mon-salon", 12L, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Retour anticipé", response.getBody().getData().commentaire());
        verify(coiffeurSalonService).mettreFinIndisponibiliteParManagerOuProprio("mon-salon", 12L, "proprio@test.com");
    }
}
