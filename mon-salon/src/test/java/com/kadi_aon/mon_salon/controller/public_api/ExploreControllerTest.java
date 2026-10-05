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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.service.RealisationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.CreneauDisponibleDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.DisponibiliteSearchDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.VitrineDisponibiliteDTORequest;
import com.kadi_aon.mon_salon.rendezvous.service.DisponibiliteService;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.service.ExploreSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.StockProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.service.StockSalonService;

@ExtendWith(MockitoExtension.class)
class ExploreControllerTest {

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

    @InjectMocks
    private ExploreController exploreController;

    private SalonDTOResponse buildSalonDTO() {
        return new SalonDTOResponse(
                1L, "Mon Salon", "mon-salon", "logo.png", "Descr", "123 Rue", "0102030405", "salon@test.com",
                2.35, 48.85, true, LocalDateTime.now(), "proprio@test.com"
        );
    }

    @Test
    void listerSalons_succes() {
        Page<SalonDTOResponse> page = new PageImpl<>(List.of(buildSalonDTO()));
        when(exploreSalonService.listerSalonsActifs(any(Pageable.class))).thenReturn(page);

        ResponseEntity<APIResponse<Page<SalonDTOResponse>>> resp =
                exploreController.listerSalons(0, 10, "nom", "asc");

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().getTotalElements());
    }

    @Test
    void rechercherSalons_succes() {
        when(exploreSalonService.rechercherSalons("paris")).thenReturn(List.of(buildSalonDTO()));

        ResponseEntity<APIResponse<List<SalonDTOResponse>>> resp = exploreController.rechercherSalons("paris");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void rechercherSalonsProches_succes() {
        when(exploreSalonService.rechercherSalonsNearby(48.85, 2.35, 5.0)).thenReturn(List.of(buildSalonDTO()));

        ResponseEntity<APIResponse<List<SalonDTOResponse>>> resp =
                exploreController.rechercherSalonsProches(48.85, 2.35, 5.0);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void filtrerParService_succes() {
        when(exploreSalonService.rechercherSalonsParService(10L)).thenReturn(List.of(buildSalonDTO()));

        ResponseEntity<APIResponse<List<SalonDTOResponse>>> resp = exploreController.filtrerParService(10L);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getSalonDetail_succes() {
        when(exploreSalonService.getSalonDetail("mon-salon")).thenReturn(buildSalonDTO());

        ResponseEntity<APIResponse<SalonDTOResponse>> resp = exploreController.getSalonDetail("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("mon-salon", resp.getBody().getData().slug());
    }

    @Test
    void getServicesSalon_succes() {
        ServiceSalonDTOResponse dto = new ServiceSalonDTOResponse(1L, "Coupe", "Descr", true, List.of(), LocalDateTime.now());
        when(serviceSalonService.listerServices("mon-salon", true)).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<ServiceSalonDTOResponse>>> resp = exploreController.getServicesSalon("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getAvisSalon_succes() {
        AvisSalonDTOResponse dto = new AvisSalonDTOResponse(
                1L, "mon-salon", "Mon Salon", 10L, "Client A", 5, "Super!", true, LocalDateTime.now(), null
        );
        when(avisSalonService.listerAvisPubliesSalon("mon-salon")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<AvisSalonDTOResponse>>> resp = exploreController.getAvisSalon("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getRealisationsSalon_succes() {
        RealisationDTOResponse dto = new RealisationDTOResponse(
                1L, "Tresses", "Tresses stylées", "https://vid.mp4", LocalDate.now(), LocalDateTime.now(),
                true, "mon-salon", "Mon Salon", 5L, "Coiff", 0, 0, 0L, LocalDateTime.now(), LocalDateTime.now()
        );
        when(realisationSalonService.listerRealisationsPubliees("mon-salon")).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<RealisationDTOResponse>>> resp = exploreController.getRealisationsSalon("mon-salon");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getDisponibilitesSalon_succes() {
        VitrineDisponibiliteDTORequest req = new VitrineDisponibiliteDTORequest(
                LocalDate.now().plusDays(2),
                List.of(1L),
                LocalTime.of(10, 0)
        );
        CreneauDisponibleDTOResponse creneau = new CreneauDisponibleDTOResponse(
                LocalTime.of(10, 0), LocalTime.of(11, 0), 60, BigDecimal.valueOf(10000), List.of()
        );
        when(disponibiliteService.calculerDisponibilites(eq("mon-salon"), any(DisponibiliteSearchDTORequest.class)))
                .thenReturn(List.of(creneau));

        ResponseEntity<APIResponse<List<CreneauDisponibleDTOResponse>>> resp =
                exploreController.getDisponibilitesSalon("mon-salon", req);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void listerProduitsTransversal_succes() {
        StockProduitDTOResponse stock = new StockProduitDTOResponse(1L, 10, 5, 20, false, LocalDateTime.now());
        ProduitDTOResponse dto = new ProduitDTOResponse(
                1L, "Huile d'avocat", "Descr", BigDecimal.valueOf(8000), "img.png", true, 2L, "Huiles", stock, LocalDateTime.now(), LocalDateTime.now()
        );
        when(stockSalonService.listerProduitsTransversal(null)).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<ProduitDTOResponse>>> resp = exploreController.listerProduitsTransversal(null);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void listerToutesRealisations_succes() {
        RealisationDTOResponse dto = new RealisationDTOResponse(
                1L, "Coiffure Star", "Descr", "https://vid.mp4", LocalDate.now(), LocalDateTime.now(),
                true, "mon-salon", "Mon Salon", 5L, "Coiff", 0, 0, 0L, LocalDateTime.now(), LocalDateTime.now()
        );
        when(realisationSalonService.listerToutesLesRealisationsPubliees()).thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<RealisationDTOResponse>>> resp = exploreController.listerToutesRealisations();

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().getData().size());
    }
}
