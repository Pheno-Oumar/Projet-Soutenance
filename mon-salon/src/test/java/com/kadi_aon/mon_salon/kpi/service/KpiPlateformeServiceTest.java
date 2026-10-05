package com.kadi_aon.mon_salon.kpi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.kpi.dto.KpiPlateformeDTOResponse;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

@ExtendWith(MockitoExtension.class)
class KpiPlateformeServiceTest {

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private RendezVousRepository rendezVousRepository;

    @Mock
    private PrestationRepository prestationRepository;

    @Mock
    private PaiementRepository paiementRepository;

    @InjectMocks
    private KpiPlateformeService kpiPlateformeService;

    @Test
    void calculerKpisPlateforme_succes() {
        when(salonRepository.count()).thenReturn(10L);
        when(salonRepository.countByStatut(true)).thenReturn(8L);
        when(salonRepository.countByStatut(false)).thenReturn(2L);

        when(compteRepository.count()).thenReturn(50L);
        when(compteRepository.countByStatut(true)).thenReturn(45L);
        when(compteRepository.countByStatut(false)).thenReturn(5L);

        when(rendezVousRepository.count()).thenReturn(120L);
        when(prestationRepository.count()).thenReturn(110L);
        when(prestationRepository.countByStatut(StatutPrestation.TERMINEE)).thenReturn(95L);

        when(paiementRepository.totalPaiementsPlateforme()).thenReturn(new BigDecimal("150000.00"));

        KpiPlateformeDTOResponse kpis = kpiPlateformeService.calculerKpisPlateforme();

        assertNotNull(kpis);
        assertEquals(10L, kpis.getNombreSalonsTotal());
        assertEquals(8L, kpis.getNombreSalonsActifs());
        assertEquals(2L, kpis.getNombreSalonsInactifs());
        assertEquals(50L, kpis.getNombreComptesTotal());
        assertEquals(45L, kpis.getNombreComptesActifs());
        assertEquals(5L, kpis.getNombreComptesInactifs());
        assertEquals(120L, kpis.getNombreRendezVousTotal());
        assertEquals(110L, kpis.getNombrePrestationsTotal());
        assertEquals(95L, kpis.getNombrePrestationsTerminees());
        assertEquals(new BigDecimal("150000.00"), kpis.getChiffreAffairesGlobal());
    }
}
