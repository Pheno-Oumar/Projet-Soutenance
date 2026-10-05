package com.kadi_aon.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kadi_aon.scheduler.entity.DemandeExportDonnees;
import com.kadi_aon.scheduler.enums.StatutExportDonnees;
import com.kadi_aon.scheduler.repository.DemandeExportDonneesRepository;

@ExtendWith(MockitoExtension.class)
class CloudinaryWorkerServiceTest {

    @Mock
    private DemandeExportDonneesRepository demandeExportDonneesRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private CloudinaryWorkerService cloudinaryWorkerService;

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
    void purgerExportAsync_succes_avecPublicId() {
        when(demandeExportDonneesRepository.findById(10L)).thenReturn(Optional.of(exportExpire));

        cloudinaryWorkerService.purgerExportAsync(10L, "rgpd_export_expired_10");

        verify(cloudinaryService).deleteRawFile("rgpd_export_expired_10");
        verify(demandeExportDonneesRepository).save(exportExpire);
        assertEquals(StatutExportDonnees.EXPIRE, exportExpire.getStatut());
    }

    @Test
    void purgerExportAsync_sansPublicId_marqueExpireSansAppelCloudinary() {
        when(demandeExportDonneesRepository.findById(10L)).thenReturn(Optional.of(exportExpire));

        cloudinaryWorkerService.purgerExportAsync(10L, null);

        verify(cloudinaryService, never()).deleteRawFile(any());
        verify(demandeExportDonneesRepository).save(exportExpire);
        assertEquals(StatutExportDonnees.EXPIRE, exportExpire.getStatut());
    }
}
