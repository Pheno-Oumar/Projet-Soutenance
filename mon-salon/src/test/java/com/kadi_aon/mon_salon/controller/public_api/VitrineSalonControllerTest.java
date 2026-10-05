package com.kadi_aon.mon_salon.controller.public_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.service.CoiffeurSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.service.RealisationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.CreneauDisponibleDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.DisponibiliteSearchDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.VitrineDisponibiliteDTORequest;
import com.kadi_aon.mon_salon.rendezvous.service.DisponibiliteService;
import com.kadi_aon.mon_salon.salon.dto.ClientRegisterDTORequest;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.service.ClientSalonService;
import com.kadi_aon.mon_salon.salon.service.ExploreSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.StockProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.service.StockSalonService;

@ExtendWith(MockitoExtension.class)
class VitrineSalonControllerTest {

    @Mock
    private ExploreSalonService exploreSalonService;
    @Mock
    private ServiceSalonService serviceSalonService;
    @Mock
    private StockSalonService stockSalonService;
    @Mock
    private AvisSalonService avisSalonService;
    @Mock
    private RealisationSalonService realisationSalonService;
    @Mock
    private DisponibiliteService disponibiliteService;
    @Mock
    private ClientSalonService clientSalonService;
    @Mock
    private CoiffeurSalonService coiffeurSalonService;
    @Mock
    private com.kadi_aon.mon_salon.story.service.StorySalonService storySalonService;
    @Mock
    private com.kadi_aon.mon_salon.stock.service.CommandeSalonService commandeSalonService;
    @Mock
    private com.kadi_aon.mon_salon.salon.service.HoraireSalonService horaireSalonService;

    @InjectMocks
    private VitrineSalonController vitrineSalonController;

    private SalonDTOResponse buildSalonDTO() {
        return new SalonDTOResponse(
                1L, "Mon Salon", "mon-salon", "logo.png", "Descr", "123 Rue", "0102030405", "salon@test.com",
                2.35, 48.85, true, LocalDateTime.now(), "proprio@test.com"
        );
    }

    @Test
    void getInfosSalon_succes() {
        when(exploreSalonService.getSalonDetail("mon-salon")).thenReturn(buildSalonDTO());

        ResponseEntity<APIResponse<SalonDTOResponse>> resp = vitrineSalonController.getInfosSalon("mon-salon");

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("mon-salon", resp.getBody().getData().slug());
    }

    @Test
    void getServices_succes() {
        ServiceSalonDTOResponse dto = new ServiceSalonDTOResponse(1L, "Coupe", "Descr", true, List.of(), LocalDateTime.now());
        when(serviceSalonService.listerServices("mon-salon", true)).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<ServiceSalonDTOResponse>>> resp = vitrineSalonController.getServices("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getCategoriesProduits_succes() {
        CategorieProduitDTOResponse dto = new CategorieProduitDTOResponse(
                1L, "Soins", "Descr", "img.png", true, "mon-salon", 0, List.of()
        );
        when(stockSalonService.listerCategories("mon-salon", true)).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<CategorieProduitDTOResponse>>> resp = vitrineSalonController.getCategoriesProduits("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getProduits_succes() {
        StockProduitDTOResponse stock = new StockProduitDTOResponse(1L, 10, 5, 20, false, LocalDateTime.now());
        ProduitDTOResponse dto = new ProduitDTOResponse(
                1L, "Shampoing", "Descr", BigDecimal.valueOf(5000), "img.png", true, 10L, "Soins", stock, LocalDateTime.now(), LocalDateTime.now()
        );
        when(stockSalonService.listerProduits("mon-salon", null, true)).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<ProduitDTOResponse>>> resp = vitrineSalonController.getProduits("mon-salon", null);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getCoiffeurs_succes() {
        ProfilCoiffeurDTOResponse dto = new ProfilCoiffeurDTOResponse(
                1L, 10L, "Coiff", "Bob", "Bob Coiff", "Bio", 5, "photo.png", "Expert coupe"
        );
        when(coiffeurSalonService.listerCoiffeursVitrine("mon-salon")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<ProfilCoiffeurDTOResponse>>> resp = vitrineSalonController.getCoiffeurs("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
        assertEquals("Bob Coiff", resp.getBody().getData().get(0).nomAffichage());
    }

    @Test
    void getDisponibilites_succes() {
        VitrineDisponibiliteDTORequest req = new VitrineDisponibiliteDTORequest(
                LocalDate.now().plusDays(1),
                List.of(101L, 102L),
                LocalTime.of(9, 0)
        );
        CreneauDisponibleDTOResponse creneau = new CreneauDisponibleDTOResponse(
                LocalTime.of(9, 0), LocalTime.of(10, 0), 60, BigDecimal.valueOf(15000), List.of()
        );
        when(disponibiliteService.calculerDisponibilites(eq("mon-salon"), any(DisponibiliteSearchDTORequest.class)))
                .thenReturn(List.of(creneau));

        ResponseEntity<APIResponse<List<CreneauDisponibleDTOResponse>>> resp =
                vitrineSalonController.getDisponibilites("mon-salon", req);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
        assertEquals(LocalTime.of(9, 0), resp.getBody().getData().get(0).heureDebut());
    }

    @Test
    void getAvis_succes() {
        AvisSalonDTOResponse dto = new AvisSalonDTOResponse(
                1L, "mon-salon", "Mon Salon", 10L, "Client A", 5, "Super!", true, LocalDateTime.now(), null
        );
        when(avisSalonService.listerAvisPubliesSalon("mon-salon")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<AvisSalonDTOResponse>>> resp = vitrineSalonController.getAvis("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getRealisations_succes() {
        RealisationDTOResponse dto = new RealisationDTOResponse(
                1L, "Dégradé", "Super coupe", "https://vid.mp4", LocalDate.now(), LocalDateTime.now(),
                true, "mon-salon", "Mon Salon", 10L, "Coiff", 0, 0, 0L, LocalDateTime.now(), LocalDateTime.now()
        );
        when(realisationSalonService.listerRealisationsPubliees("mon-salon")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<RealisationDTOResponse>>> resp = vitrineSalonController.getRealisations("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void register_succes() {
        ClientRegisterDTORequest req = new ClientRegisterDTORequest(
                "Dupont", "Jean", "jean@test.com", "770000000", "password123", LocalDate.of(1990, 1, 1)
        );
        CompteDTOResponse dto = new CompteDTOResponse(
                1L, "Dupont", "Jean", LocalDate.of(1990, 1, 1), "jean@test.com", "770000000", true, null, LocalDateTime.now()
        );
        when(clientSalonService.enregistrerClientDansSalon("mon-salon", req)).thenReturn(dto);

        ResponseEntity<APIResponse<CompteDTOResponse>> resp = vitrineSalonController.register("mon-salon", req);

        assertNotNull(resp);
        assertEquals(201, resp.getStatusCode().value());
        assertEquals("jean@test.com", resp.getBody().getData().email());
    }

    @Test
    void getStories_succes() {
        com.kadi_aon.mon_salon.story.dto.StoryDTOResponse story = new com.kadi_aon.mon_salon.story.dto.StoryDTOResponse(
                1L, "https://cloudinary.com/story.mp4", com.kadi_aon.mon_salon.story.enums.TypeMediaStory.VIDEO,
                LocalDateTime.now(), LocalDateTime.now().plusHours(24), "mon-salon", "Mon Salon", "logo.png"
        );
        when(storySalonService.listerStoriesSalon("mon-salon")).thenReturn(List.of(story));

        ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.story.dto.StoryDTOResponse>>> resp =
                vitrineSalonController.getStories("mon-salon");

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
        assertEquals(1L, resp.getBody().getData().get(0).id());
    }

    @Test
    void getHoraires_succes() {
        com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTOResponse horaire = new com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTOResponse(
                1L, com.kadi_aon.mon_salon.salon.enums.JourSemaine.LUNDI,
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(19, 0),
                null, null, true
        );
        when(horaireSalonService.listerHoraires("mon-salon")).thenReturn(List.of(horaire));

        ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTOResponse>>> resp =
                vitrineSalonController.getHoraires("mon-salon");

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
        assertEquals(com.kadi_aon.mon_salon.salon.enums.JourSemaine.LUNDI, resp.getBody().getData().get(0).jourSemaine());
    }

    @Test
    void passerCommandeVitrine_succes() {
        var req = new com.kadi_aon.mon_salon.stock.dto.VitrineCommandeDTORequest(
                "Client", "Test", "+223 70 00 00 00", "client@test.com", null,
                List.of(new com.kadi_aon.mon_salon.stock.dto.VitrineCommandeDTORequest.LigneVitrineCommandeDTORequest(1L, 2))
        );
        var expected = new com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse(
                10L, "CMD-001", LocalDateTime.now(), "EN_ATTENTE", new java.math.BigDecimal("5000"),
                "Client Test", "client@test.com", null, null, null, null, null, null, List.of()
        );
        when(commandeSalonService.passerCommandeDepuisVitrine(eq("mon-salon"), any())).thenReturn(expected);

        var resp = vitrineSalonController.passerCommandeVitrine("mon-salon", req);

        assertNotNull(resp);
        assertEquals(201, resp.getStatusCode().value());
        assertEquals("CMD-001", resp.getBody().getData().numeroCommande());
    }
}
