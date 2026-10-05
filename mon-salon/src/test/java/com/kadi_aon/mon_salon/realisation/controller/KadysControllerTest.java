package com.kadi_aon.mon_salon.realisation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import com.kadi_aon.mon_salon.realisation.dto.CommentaireCreateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.CommentaireDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.KadysRealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.LikeToggleDTOResponse;
import com.kadi_aon.mon_salon.realisation.enums.StatutCommentaire;
import com.kadi_aon.mon_salon.realisation.service.KadysInteractionService;
import com.kadi_aon.mon_salon.story.dto.SalonStoriesDTOResponse;
import com.kadi_aon.mon_salon.story.service.StorySalonService;

@ExtendWith(MockitoExtension.class)
class KadysControllerTest {

    @Mock
    private KadysInteractionService kadysInteractionService;

    @Mock
    private StorySalonService storySalonService;

    @InjectMocks
    private KadysController kadysController;

    private Principal principal;
    private KadysRealisationDTOResponse realisationDTO;

    @BeforeEach
    void setUp() {
        principal = () -> "fatou@client.com";
        realisationDTO = new KadysRealisationDTOResponse(
                10L, "Look Tendance", "Description", "https://video.mp4",
                LocalDate.now(), LocalDateTime.now(), "mon-salon", "Mon Salon", "logo.png",
                2L, "Amadou Coiffeur", 12, 3, 150L, false
        );
    }

    @Test
    @DisplayName("GET /kadys/feed : retourne le flux paginé")
    void testGetFeed() {
        Page<KadysRealisationDTOResponse> page = new PageImpl<>(List.of(realisationDTO));
        when(kadysInteractionService.listerFeedKadys(eq("fatou@client.com"), any())).thenReturn(page);

        ResponseEntity<Page<KadysRealisationDTOResponse>> response = kadysController.getFeed(0, 10, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    @DisplayName("GET /kadys/{id} : retourne le détail d'une réalisation")
    void testGetRealisation() {
        when(kadysInteractionService.obtenirRealisationKadys(10L, "fatou@client.com")).thenReturn(realisationDTO);

        ResponseEntity<KadysRealisationDTOResponse> response = kadysController.getRealisation(10L, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(10L, response.getBody().id());
    }

    @Test
    @DisplayName("GET /kadys/{id}/commentaires : retourne les commentaires actifs")
    void testGetCommentaires() {
        CommentaireDTOResponse comment = new CommentaireDTOResponse(
                1L, 10L, 5L, "Fatou Diallo", null, "Génial", StatutCommentaire.ACTIF, LocalDateTime.now(), true
        );
        when(kadysInteractionService.listerCommentaires(10L, "fatou@client.com")).thenReturn(List.of(comment));

        ResponseEntity<List<CommentaireDTOResponse>> response = kadysController.getCommentaires(10L, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("POST /kadys/{id}/vue : enregistre le visionnage")
    void testEnregistrerVue() {
        ResponseEntity<Void> response = kadysController.enregistrerVue(10L);

        assertEquals(200, response.getStatusCode().value());
        verify(kadysInteractionService).enregistrerVue(10L);
    }

    @Test
    @DisplayName("GET /kadys/stories : retourne les stories groupées par salon")
    void testGetStoriesPourKadys() {
        SalonStoriesDTOResponse salonStories = new SalonStoriesDTOResponse(
                1L, "Salon Kady", "salon-kady", "logo.webp", 2, List.of()
        );
        when(storySalonService.listerStoriesTousSalonsPourKadys()).thenReturn(List.of(salonStories));

        ResponseEntity<List<SalonStoriesDTOResponse>> response = kadysController.getStoriesPourKadys();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("POST /kadys/{id}/like : toggle le like")
    void testToggleLike() {
        LikeToggleDTOResponse toggleResponse = new LikeToggleDTOResponse(true, 13);
        when(kadysInteractionService.toggleLike(10L, "fatou@client.com")).thenReturn(toggleResponse);

        ResponseEntity<LikeToggleDTOResponse> response = kadysController.toggleLike(10L, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, response.getBody().liked());
        assertEquals(13, response.getBody().totalLikes());
    }

    @Test
    @DisplayName("POST /kadys/{id}/commentaires : ajoute un commentaire (201 Created)")
    void testAjouterCommentaire() {
        CommentaireCreateDTORequest req = new CommentaireCreateDTORequest("Top!");
        CommentaireDTOResponse comment = new CommentaireDTOResponse(
                1L, 10L, 5L, "Fatou Diallo", null, "Top!", StatutCommentaire.ACTIF, LocalDateTime.now(), true
        );
        when(kadysInteractionService.ajouterCommentaire(10L, "fatou@client.com", req)).thenReturn(comment);

        ResponseEntity<CommentaireDTOResponse> response = kadysController.ajouterCommentaire(10L, req, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("Top!", response.getBody().contenu());
    }

    @Test
    @DisplayName("DELETE /kadys/commentaires/{id} : supprime un commentaire (204 No Content)")
    void testSupprimerCommentaire() {
        ResponseEntity<Void> response = kadysController.supprimerCommentaire(1L, principal);

        assertEquals(204, response.getStatusCode().value());
        verify(kadysInteractionService).supprimerCommentaire(1L, "fatou@client.com");
    }

    @Test
    @DisplayName("GET /client/inspirations : liste les réalisations likées")
    void testGetInspirations() {
        Page<KadysRealisationDTOResponse> page = new PageImpl<>(List.of(realisationDTO));
        when(kadysInteractionService.listerMesInspirations(eq("fatou@client.com"), any())).thenReturn(page);

        ResponseEntity<Page<KadysRealisationDTOResponse>> response = kadysController.getInspirations(0, 10, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getTotalElements());
    }
}
