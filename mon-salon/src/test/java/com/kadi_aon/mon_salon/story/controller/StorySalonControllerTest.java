package com.kadi_aon.mon_salon.story.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.kadi_aon.mon_salon.realisation.service.KadysInteractionService;
import com.kadi_aon.mon_salon.story.dto.StoryDTOResponse;
import com.kadi_aon.mon_salon.story.enums.TypeMediaStory;
import com.kadi_aon.mon_salon.story.service.StorySalonService;

@ExtendWith(MockitoExtension.class)
class StorySalonControllerTest {

    @Mock
    private StorySalonService storySalonService;

    @Mock
    private KadysInteractionService kadysInteractionService;

    @InjectMocks
    private StorySalonController storySalonController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = () -> "manager@kady.com";
    }

    @Test
    @DisplayName("POST /{slugSalon}/manager/stories : publier une story (201 Created)")
    void testPublierStory() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "story.jpg", "image/jpeg", "image bytes".getBytes());
        StoryDTOResponse story = new StoryDTOResponse(
                10L, "https://cloudinary.com/story.webp", TypeMediaStory.IMAGE,
                LocalDateTime.now(), LocalDateTime.now().plusHours(24), "salon-kady", "Salon Kady", "logo.webp"
        );

        when(storySalonService.publierStory("salon-kady", "manager@kady.com", file)).thenReturn(story);

        ResponseEntity<StoryDTOResponse> response = storySalonController.publierStory("salon-kady", file, principal);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals(10L, response.getBody().id());
    }

    @Test
    @DisplayName("GET /{slugSalon}/manager/stories : lister les stories actives")
    void testListerStories() {
        StoryDTOResponse story = new StoryDTOResponse(
                10L, "https://cloudinary.com/story.webp", TypeMediaStory.IMAGE,
                LocalDateTime.now(), LocalDateTime.now().plusHours(24), "salon-kady", "Salon Kady", "logo.webp"
        );

        when(storySalonService.listerStoriesSalon("salon-kady")).thenReturn(List.of(story));

        ResponseEntity<List<StoryDTOResponse>> response = storySalonController.listerStories("salon-kady");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("DELETE /{slugSalon}/manager/stories/{id} : supprimer une story (204 No Content)")
    void testSupprimerStory() {
        ResponseEntity<Void> response = storySalonController.supprimerStory("salon-kady", 10L, principal);

        assertEquals(204, response.getStatusCode().value());
        verify(storySalonService).supprimerStory("salon-kady", 10L, "manager@kady.com");
    }

    @Test
    @DisplayName("PATCH /{slugSalon}/manager/kadys/commentaires/{id}/masquer : masquer un commentaire (200 OK)")
    void testMasquerCommentaire() {
        ResponseEntity<Void> response = storySalonController.masquerCommentaire("salon-kady", 5L, principal);

        assertEquals(200, response.getStatusCode().value());
        verify(kadysInteractionService).masquerCommentaire("salon-kady", 5L, "manager@kady.com");
    }
}
