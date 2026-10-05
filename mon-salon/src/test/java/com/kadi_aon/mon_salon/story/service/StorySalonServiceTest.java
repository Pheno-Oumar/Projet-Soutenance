package com.kadi_aon.mon_salon.story.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.story.dto.SalonStoriesDTOResponse;
import com.kadi_aon.mon_salon.story.dto.StoryDTOResponse;
import com.kadi_aon.mon_salon.story.entity.StorySalon;
import com.kadi_aon.mon_salon.story.enums.TypeMediaStory;
import com.kadi_aon.mon_salon.story.repository.StorySalonRepository;

@ExtendWith(MockitoExtension.class)
class StorySalonServiceTest {

    @Mock
    private StorySalonRepository storySalonRepository;
    @Mock
    private AffectationSalonRepository affectationSalonRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private StorySalonService storySalonService;

    private Salon salon;
    private Compte managerCompte;
    private AffectationSalon affectationManager;

    @BeforeEach
    void setUp() {
        salon = Salon.builder()
                .id(1L)
                .nom("Salon Kady")
                .slug("salon-kady")
                .logoUrl("https://cloudinary.com/logo.webp")
                .statut(true)
                .build();

        managerCompte = Compte.builder()
                .id(5L)
                .nom("Sow")
                .prenom("Aissatou")
                .email("manager@kady.com")
                .build();

        affectationManager = AffectationSalon.builder()
                .id(12L)
                .compte(managerCompte)
                .salon(salon)
                .statut(true)
                .roles(new HashSet<>(Set.of(RoleSalon.builder().role(TypeRoleSalon.MANAGER).build())))
                .build();
    }

    @Test
    @DisplayName("Publier Story : upload Cloudinary et expiration à 24h")
    void testPublierStory() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "story.mp4", "video/mp4", "video bytes".getBytes());

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@kady.com", "salon-kady"))
                .thenReturn(Optional.of(affectationManager));
        when(cloudinaryService.uploadMediaStory(file, "salon-kady"))
                .thenReturn("https://cloudinary.com/story.mp4");

        StorySalon savedStory = StorySalon.builder()
                .id(101L)
                .salon(salon)
                .auteur(managerCompte)
                .mediaUrl("https://cloudinary.com/story.mp4")
                .typeMedia(TypeMediaStory.VIDEO)
                .dateCreation(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusHours(24))
                .actif(true)
                .build();

        when(storySalonRepository.save(any(StorySalon.class))).thenReturn(savedStory);

        StoryDTOResponse response = storySalonService.publierStory("salon-kady", "manager@kady.com", file);

        assertNotNull(response);
        assertEquals(101L, response.id());
        assertEquals("https://cloudinary.com/story.mp4", response.mediaUrl());
        assertEquals(TypeMediaStory.VIDEO, response.typeMedia());
        verify(cloudinaryService).uploadMediaStory(file, "salon-kady");
        verify(storySalonRepository).save(any(StorySalon.class));
        verify(auditLogService).logActionSalon(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Supprimer Story : supprime sur Cloudinary et en BDD")
    void testSupprimerStory() {
        StorySalon story = StorySalon.builder()
                .id(101L)
                .salon(salon)
                .auteur(managerCompte)
                .mediaUrl("https://cloudinary.com/story.mp4")
                .typeMedia(TypeMediaStory.VIDEO)
                .build();

        when(affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue("manager@kady.com", "salon-kady"))
                .thenReturn(Optional.of(affectationManager));
        when(storySalonRepository.findByIdAndSalonSlug(101L, "salon-kady")).thenReturn(Optional.of(story));

        storySalonService.supprimerStory("salon-kady", 101L, "manager@kady.com");

        verify(cloudinaryService).deleteMediaByUrl("https://cloudinary.com/story.mp4", "video");
        verify(storySalonRepository).delete(story);
        verify(auditLogService).logActionSalon(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lister Stories Salon : retourne les stories actives")
    void testListerStoriesSalon() {
        when(salonRepository.existsBySlug("salon-kady")).thenReturn(true);

        StorySalon story = StorySalon.builder()
                .id(101L)
                .salon(salon)
                .mediaUrl("https://cloudinary.com/story.webp")
                .typeMedia(TypeMediaStory.IMAGE)
                .dateCreation(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusHours(24))
                .actif(true)
                .build();

        when(storySalonRepository.findActiveStoriesBySalonSlug(eq("salon-kady"), any(LocalDateTime.class)))
                .thenReturn(List.of(story));

        List<StoryDTOResponse> stories = storySalonService.listerStoriesSalon("salon-kady");

        assertNotNull(stories);
        assertEquals(1, stories.size());
        assertEquals(101L, stories.get(0).id());
    }

    @Test
    @DisplayName("Lister Stories pour Kady's : regroupe les stories par salon pour les cercles")
    void testListerStoriesTousSalonsPourKadys() {
        StorySalon story1 = StorySalon.builder()
                .id(101L)
                .salon(salon)
                .mediaUrl("https://cloudinary.com/story1.webp")
                .typeMedia(TypeMediaStory.IMAGE)
                .dateCreation(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusHours(24))
                .actif(true)
                .build();

        when(storySalonRepository.findAllActiveStoriesWithSalon(any(LocalDateTime.class)))
                .thenReturn(List.of(story1));

        List<SalonStoriesDTOResponse> grouped = storySalonService.listerStoriesTousSalonsPourKadys();

        assertNotNull(grouped);
        assertEquals(1, grouped.size());
        assertEquals("Salon Kady", grouped.get(0).salonNom());
        assertEquals(1, grouped.get(0).nombreStories());
    }
}
