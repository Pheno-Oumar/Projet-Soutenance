package com.kadi_aon.scheduler.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.kadi_aon.scheduler.entity.DemandeExportDonnees;
import com.kadi_aon.scheduler.enums.StatutExportDonnees;
import com.kadi_aon.scheduler.repository.DemandeExportDonneesRepository;

@ExtendWith(MockitoExtension.class)
class CloudinaryPurgeSchedulerTest {

    @Mock
    private DemandeExportDonneesRepository demandeExportDonneesRepository;

    @Mock
    private CloudinaryWorkerService cloudinaryWorkerService;

    @InjectMocks
    private CloudinaryPurgeScheduler cloudinaryPurgeScheduler;

    private DemandeExportDonnees exportExpire;

    @BeforeEach
    void setUp() {
        exportExpire = DemandeExportDonnees.builder()
                .id(10L)
                .compteId(5L)
                .format("JSON")
                .cloudinaryPublicId("rgpd_export_expired_10")
                .urlTelechargement("https://cloudinary.com/raw/upload/rgpd_export_expired_10.json")
                .statut(StatutExportDonnees.DISPONIBLE)
                .dateDemande(LocalDateTime.now().minusDays(3))
                .dateExpiration(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Test
    void purgerFichiersExpires_succes_captureEtDelegueAuWorker() {
        when(demandeExportDonneesRepository.findByStatutAndDateExpirationBefore(
                eq(StatutExportDonnees.DISPONIBLE), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(exportExpire));

        cloudinaryPurgeScheduler.purgerFichiersExpires();

        verify(cloudinaryWorkerService).purgerExportAsync(10L, "rgpd_export_expired_10");
    }

    @Test
    void purgerFichiersExpires_aucunExport_neFaitRien() {
        when(demandeExportDonneesRepository.findByStatutAndDateExpirationBefore(
                eq(StatutExportDonnees.DISPONIBLE), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        cloudinaryPurgeScheduler.purgerFichiersExpires();

        verify(cloudinaryWorkerService, never()).purgerExportAsync(any(), any());
    }
}

