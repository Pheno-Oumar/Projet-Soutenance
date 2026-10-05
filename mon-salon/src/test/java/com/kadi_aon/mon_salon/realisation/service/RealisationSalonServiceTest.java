package com.kadi_aon.mon_salon.realisation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.realisation.dto.RealisationCreateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.RealisationUpdateDTORequest;
import com.kadi_aon.mon_salon.realisation.entity.Realisation;
import com.kadi_aon.mon_salon.realisation.repository.RealisationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class RealisationSalonServiceTest {

    @Mock
    private RealisationRepository realisationRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private CompteRepository compteRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RealisationSalonService realisationSalonService;

    private Salon salon;
    private Compte manager;
    private Compte coiffeur;
    private AffectationSalon affectationManager;
    private AffectationSalon affectationCoiffeur;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).slug("salon-chic").nom("Salon Chic").build();
        manager = Compte.builder().id(20L).nom("Boss").prenom("Marc").email("manager@test.com").build();
        coiffeur = Compte.builder().id(30L).nom("Barber").prenom("Bob").email("bob@test.com").build();

        RoleSalon roleManager = RoleSalon.builder().id(1L).role(TypeRoleSalon.MANAGER).build();
        Set<RoleSalon> rolesManager = new HashSet<>();
        rolesManager.add(roleManager);

        affectationManager = AffectationSalon.builder()
                .id(200L)
                .salon(salon)
                .compte(manager)
                .roles(rolesManager)
                .statut(true)
                .build();

        RoleSalon roleCoiffeur = RoleSalon.builder().id(2L).role(TypeRoleSalon.COIFFEUR).build();
        Set<RoleSalon> rolesCoiffeur = new HashSet<>();
        rolesCoiffeur.add(roleCoiffeur);

        affectationCoiffeur = AffectationSalon.builder()
                .id(300L)
                .salon(salon)
                .compte(coiffeur)
                .roles(rolesCoiffeur)
                .statut(true)
                .build();
    }

    @Test
    void creerRealisation_avecVideo_succes() throws IOException {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationManager));
        when(compteRepository.findById(30L)).thenReturn(Optional.of(coiffeur));
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("bob@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationCoiffeur));

        MultipartFile videoMock = mock(MultipartFile.class);
        when(videoMock.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadVideoRealisation(videoMock, "salon-chic"))
                .thenReturn("https://res.cloudinary.com/test/video/upload/v1/realisations/video1.mp4");

        when(realisationRepository.save(any(Realisation.class))).thenAnswer(invocation -> {
            Realisation r = invocation.getArgument(0);
            r.setId(10L);
            return r;
        });

        RealisationCreateDTORequest request = new RealisationCreateDTORequest(
                "Dégradé américain", "Super coupe", 30L, LocalDate.now(), true
        );

        RealisationDTOResponse response = realisationSalonService.creerRealisation(
                "salon-chic", "manager@test.com", request, videoMock
        );

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Dégradé américain", response.titre());
        assertEquals("https://res.cloudinary.com/test/video/upload/v1/realisations/video1.mp4", response.urlVideo());
        assertTrue(response.statutPublication());
        assertNotNull(response.datePublication());
    }

    @Test
    void creerRealisation_sansVideo_lanceIllegalArgumentException() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationManager));

        RealisationCreateDTORequest request = new RealisationCreateDTORequest(
                "Dégradé américain", "Super coupe", 30L, LocalDate.now(), true
        );

        assertThrows(IllegalArgumentException.class, () ->
                realisationSalonService.creerRealisation("salon-chic", "manager@test.com", request, null));
    }

    @Test
    void modifierRealisation_avecNouvelleVideo_supprimeAncienne() throws IOException {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationManager));

        Realisation existant = Realisation.builder()
                .id(10L)
                .salon(salon)
                .titre("Ancien titre")
                .urlVideo("https://res.cloudinary.com/test/video/upload/v1/realisations/ancienne.mp4")
                .build();

        when(realisationRepository.findByIdAndSalonSlug(10L, "salon-chic"))
                .thenReturn(Optional.of(existant));

        MultipartFile nouvelleVideo = mock(MultipartFile.class);
        when(nouvelleVideo.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadVideoRealisation(nouvelleVideo, "salon-chic"))
                .thenReturn("https://res.cloudinary.com/test/video/upload/v1/realisations/nouvelle.mp4");

        when(realisationRepository.save(any(Realisation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RealisationUpdateDTORequest request = new RealisationUpdateDTORequest(
                "Nouveau titre", "Nouvelle desc", null, null
        );

        RealisationDTOResponse response = realisationSalonService.modifierRealisation(
                "salon-chic", 10L, "manager@test.com", request, nouvelleVideo
        );

        assertNotNull(response);
        assertEquals("Nouveau titre", response.titre());
        assertEquals("https://res.cloudinary.com/test/video/upload/v1/realisations/nouvelle.mp4", response.urlVideo());
        // 🔥 Vérifie la suppression de l'ancienne vidéo sur Cloudinary
        verify(cloudinaryService).deleteMediaByUrl("https://res.cloudinary.com/test/video/upload/v1/realisations/ancienne.mp4", "video");
    }

    @Test
    void modifierStatutPublication_succes() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationManager));

        Realisation existant = Realisation.builder()
                .id(10L)
                .salon(salon)
                .titre("Coupe")
                .statutPublication(false)
                .build();

        when(realisationRepository.findByIdAndSalonSlug(10L, "salon-chic"))
                .thenReturn(Optional.of(existant));
        when(realisationRepository.save(any(Realisation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RealisationDTOResponse response = realisationSalonService.modifierStatutPublication(
                "salon-chic", 10L, "manager@test.com", true
        );

        assertNotNull(response);
        assertTrue(response.statutPublication());
        assertNotNull(response.datePublication());
    }

    @Test
    void supprimerRealisation_supprimeVideoCloudinary() {
        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@test.com", "salon-chic"))
                .thenReturn(Optional.of(affectationManager));

        Realisation existant = Realisation.builder()
                .id(10L)
                .salon(salon)
                .titre("Coupe à supprimer")
                .urlVideo("https://res.cloudinary.com/test/video/upload/v1/realisations/a_supprimer.mp4")
                .build();

        when(realisationRepository.findByIdAndSalonSlug(10L, "salon-chic"))
                .thenReturn(Optional.of(existant));

        realisationSalonService.supprimerRealisation("salon-chic", 10L, "manager@test.com");

        verify(cloudinaryService).deleteMediaByUrl("https://res.cloudinary.com/test/video/upload/v1/realisations/a_supprimer.mp4", "video");
        verify(realisationRepository).delete(existant);
    }

    @Test
    void listerRealisationsPubliees_client_seulementStatutPublicationTrue() {
        when(salonRepository.existsBySlug("salon-chic")).thenReturn(true);

        Realisation rPubliee = Realisation.builder()
                .id(1L)
                .salon(salon)
                .titre("Coupe Publiée")
                .statutPublication(true)
                .datePublication(LocalDateTime.now())
                .build();

        when(realisationRepository.findBySalonSlugAndStatutPublicationTrueOrderByDatePublicationDesc("salon-chic"))
                .thenReturn(List.of(rPubliee));

        List<RealisationDTOResponse> list = realisationSalonService.listerRealisationsPubliees("salon-chic");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue(list.get(0).statutPublication());
    }

    @Test
    void obtenirRealisationPubliee_nonPubliee_lanceEntityNotFoundException() {
        when(realisationRepository.findByIdAndSalonSlugAndStatutPublicationTrue(99L, "salon-chic"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                realisationSalonService.obtenirRealisationPubliee("salon-chic", 99L));
    }
}
