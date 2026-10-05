package com.kadi_aon.mon_salon.controller.comptable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.facturation.dto.KpiFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportExportDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RapportFinancierFiltreDTORequest;
import com.kadi_aon.mon_salon.facturation.enums.TypeRapportFinancier;
import com.kadi_aon.mon_salon.facturation.service.RapportFinancierSalonService;
import com.kadi_aon.mon_salon.rgpd.enums.FormatExportDonnees;

@ExtendWith(MockitoExtension.class)
class ComptableRapportControllerTest {

    @Mock
    private RapportFinancierSalonService rapportFinancierSalonService;

    @InjectMocks
    private ComptableRapportController comptableRapportController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        org.mockito.Mockito.lenient().when(principal.getName()).thenReturn("comptable@test.com");
    }

    @Test
    void consulterRapport_succes() {
        RapportFinancierFiltreDTORequest request = new RapportFinancierFiltreDTORequest(
                LocalDate.now().minusDays(7), LocalDate.now(), TypeRapportFinancier.ENTREES_SORTIES,
                null, null, FormatExportDonnees.PDF
        );

        RapportFinancierDTOResponse dto = RapportFinancierDTOResponse.builder()
                .slugSalon("mon-salon")
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .typeRapport(TypeRapportFinancier.ENTREES_SORTIES)
                .totalEntrees(new BigDecimal("500.00"))
                .totalSorties(new BigDecimal("150.00"))
                .soldeNet(new BigDecimal("350.00"))
                .nombreEntrees(5)
                .nombreSorties(2)
                .entrees(List.of())
                .sorties(List.of())
                .build();

        when(rapportFinancierSalonService.consulterRapportFinancier("mon-salon", request)).thenReturn(dto);

        ResponseEntity<APIResponse<RapportFinancierDTOResponse>> response =
                comptableRapportController.consulterRapport("mon-salon", request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(new BigDecimal("350.00"), response.getBody().getData().soldeNet());
        verify(rapportFinancierSalonService).consulterRapportFinancier("mon-salon", request);
    }

    @Test
    void exporterRapport_succes() {
        RapportFinancierFiltreDTORequest request = new RapportFinancierFiltreDTORequest(
                LocalDate.now().minusDays(7), LocalDate.now(), TypeRapportFinancier.ENTREES_SORTIES,
                null, null, FormatExportDonnees.PDF
        );

        RapportExportDTOResponse dto = RapportExportDTOResponse.builder()
                .exportId(10L)
                .slugSalon("mon-salon")
                .format(FormatExportDonnees.PDF)
                .urlTelechargement("https://cloudinary.com/rapport.pdf")
                .dateDemande(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusHours(24))
                .statut("DISPONIBLE")
                .build();

        when(rapportFinancierSalonService.exporterRapportFinancier("mon-salon", "comptable@test.com", request))
                .thenReturn(dto);

        ResponseEntity<APIResponse<RapportExportDTOResponse>> response =
                comptableRapportController.exporterRapport("mon-salon", request, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("https://cloudinary.com/rapport.pdf", response.getBody().getData().urlTelechargement());
        verify(rapportFinancierSalonService).exporterRapportFinancier("mon-salon", "comptable@test.com", request);
    }

    @Test
    void getKpiFinanciers_succes() {
        KpiFinancierDTOResponse dto = KpiFinancierDTOResponse.builder()
                .slugSalon("mon-salon")
                .chiffreAffairesTotal(new BigDecimal("10000.00"))
                .revenusMoisEnCours(new BigDecimal("2500.00"))
                .depensesMoisEnCours(new BigDecimal("1000.00"))
                .beneficeNetMoisEnCours(new BigDecimal("1500.00"))
                .revenusAujourdhui(new BigDecimal("300.00"))
                .depensesAujourdhui(new BigDecimal("50.00"))
                .beneficeNetAujourdhui(new BigDecimal("250.00"))
                .totalRemboursements(new BigDecimal("100.00"))
                .depensesParCategorie(Map.of("LOYER", new BigDecimal("500.00")))
                .sessionCaisseOuverte(true)
                .soldeTheoriqueCaisseActive(new BigDecimal("450.00"))
                .build();

        when(rapportFinancierSalonService.obtenirKpiFinanciers("mon-salon")).thenReturn(dto);

        ResponseEntity<APIResponse<KpiFinancierDTOResponse>> response =
                comptableRapportController.getKpiFinanciers("mon-salon");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(new BigDecimal("10000.00"), response.getBody().getData().chiffreAffairesTotal());
        verify(rapportFinancierSalonService).obtenirKpiFinanciers("mon-salon");
    }

    @Test
    void listerMesExports_succes() {
        RapportExportDTOResponse dto = RapportExportDTOResponse.builder()
                .exportId(1L)
                .slugSalon("mon-salon")
                .format(FormatExportDonnees.PDF)
                .urlTelechargement("https://cloudinary.com/rapport.pdf")
                .dateDemande(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusHours(24))
                .statut("DISPONIBLE")
                .build();

        when(rapportFinancierSalonService.listerMesExportsFinanciers("comptable@test.com", "mon-salon"))
                .thenReturn(List.of(dto));

        ResponseEntity<APIResponse<List<RapportExportDTOResponse>>> response =
                comptableRapportController.listerMesExports("mon-salon", principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        verify(rapportFinancierSalonService).listerMesExportsFinanciers("comptable@test.com", "mon-salon");
    }
}
