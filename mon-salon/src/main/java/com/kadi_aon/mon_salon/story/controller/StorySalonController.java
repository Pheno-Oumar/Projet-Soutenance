package com.kadi_aon.mon_salon.story.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.realisation.service.KadysInteractionService;
import com.kadi_aon.mon_salon.story.dto.StoryDTOResponse;
import com.kadi_aon.mon_salon.story.service.StorySalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/manager")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'MANAGER')")
@RequiredArgsConstructor
@Tag(name = "Manager Stories & Modération Kady's", description = "Gestion des stories temporaires 24h et modération des commentaires")
public class StorySalonController {

    private final StorySalonService storySalonService;
    private final KadysInteractionService kadysInteractionService;

    @PostMapping("/stories")
    @Operation(summary = "Publier une nouvelle story de 24h pour le salon")
    public ResponseEntity<StoryDTOResponse> publierStory(
            @PathVariable String slugSalon,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {

        StoryDTOResponse story = storySalonService.publierStory(slugSalon, principal.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(story);
    }

    @GetMapping("/stories")
    @Operation(summary = "Lister les stories actives du salon")
    public ResponseEntity<List<StoryDTOResponse>> listerStories(
            @PathVariable String slugSalon) {

        List<StoryDTOResponse> stories = storySalonService.listerStoriesSalon(slugSalon);
        return ResponseEntity.ok(stories);
    }

    @DeleteMapping("/stories/{id}")
    @Operation(summary = "Supprimer manuellement une story du salon")
    public ResponseEntity<Void> supprimerStory(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        storySalonService.supprimerStory(slugSalon, id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/kadys/commentaires")
    @Operation(summary = "Lister tous les commentaires sous les réalisations du salon pour modération")
    public ResponseEntity<List<com.kadi_aon.mon_salon.realisation.dto.CommentaireDTOResponse>> listerCommentaires(
            @PathVariable String slugSalon,
            Principal principal) {

        List<com.kadi_aon.mon_salon.realisation.dto.CommentaireDTOResponse> commentaires =
                kadysInteractionService.listerCommentairesSalon(slugSalon, principal.getName());
        return ResponseEntity.ok(commentaires);
    }

    @PatchMapping("/kadys/commentaires/{id}/masquer")
    @Operation(summary = "Masquer un commentaire sous une réalisation du salon")
    public ResponseEntity<Void> masquerCommentaire(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        kadysInteractionService.masquerCommentaire(slugSalon, id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/kadys/commentaires/{id}/demasquer")
    @Operation(summary = "Réactiver un commentaire masqué sous une réalisation du salon")
    public ResponseEntity<Void> deMasquerCommentaire(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        kadysInteractionService.deMasquerCommentaire(slugSalon, id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/kadys/commentaires/{id}")
    @Operation(summary = "Supprimer définitivement un commentaire sous une réalisation du salon")
    public ResponseEntity<Void> supprimerCommentaire(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        kadysInteractionService.supprimerCommentaireParManager(slugSalon, id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
