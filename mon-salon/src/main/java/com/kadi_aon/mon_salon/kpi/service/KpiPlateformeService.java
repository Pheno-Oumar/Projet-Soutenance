package com.kadi_aon.mon_salon.kpi.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.facturation.repository.PaiementRepository;
import com.kadi_aon.mon_salon.kpi.dto.KpiPlateformeDTOResponse;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiPlateformeService {

    private final SalonRepository salonRepository;
    private final CompteRepository compteRepository;
    private final RendezVousRepository rendezVousRepository;
    private final PrestationRepository prestationRepository;
    private final PaiementRepository paiementRepository;

    public KpiPlateformeDTOResponse calculerKpisPlateforme() {
        long nombreSalonsTotal = salonRepository.count();
        long nombreSalonsActifs = salonRepository.countByStatut(true);
        long nombreSalonsInactifs = salonRepository.countByStatut(false);

        long nombreComptesTotal = compteRepository.count();
        long nombreComptesActifs = compteRepository.countByStatut(true);
        long nombreComptesInactifs = compteRepository.countByStatut(false);

        long nombreRendezVousTotal = rendezVousRepository.count();
        long nombrePrestationsTotal = prestationRepository.count();
        long nombrePrestationsTerminees = prestationRepository.countByStatut(StatutPrestation.TERMINEE);

        BigDecimal chiffreAffairesGlobal = paiementRepository.totalPaiementsPlateforme();
        if (chiffreAffairesGlobal == null) {
            chiffreAffairesGlobal = BigDecimal.ZERO;
        }

        return KpiPlateformeDTOResponse.builder()
                .nombreSalonsTotal(nombreSalonsTotal)
                .nombreSalonsActifs(nombreSalonsActifs)
                .nombreSalonsInactifs(nombreSalonsInactifs)
                .nombreComptesTotal(nombreComptesTotal)
                .nombreComptesActifs(nombreComptesActifs)
                .nombreComptesInactifs(nombreComptesInactifs)
                .nombreRendezVousTotal(nombreRendezVousTotal)
                .nombrePrestationsTotal(nombrePrestationsTotal)
                .nombrePrestationsTerminees(nombrePrestationsTerminees)
                .chiffreAffairesGlobal(chiffreAffairesGlobal)
                .build();
    }
}
