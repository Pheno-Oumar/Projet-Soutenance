package com.kadi_aon.mon_salon.realisation.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.realisation.dto.CommentaireCreateDTORequest;
import com.kadi_aon.mon_salon.realisation.dto.CommentaireDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.KadysRealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.dto.LikeToggleDTOResponse;
import com.kadi_aon.mon_salon.realisation.service.KadysInteractionService;
import com.kadi_aon.mon_salon.story.dto.SalonStoriesDTOResponse;
import com.kadi_aon.mon_salon.story.service.StorySalonService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class KadysController {

    private final KadysInteractionService kadysInteractionService;
    private final StorySalonService storySalonService;

    // ==========================================
    // 1. FLUX PUBLIC KADY'S (TIKTOK-LIKE)
    // ==========================================

    @GetMapping("/kadys/feed")
    public ResponseEntity<Page<KadysRealisationDTOResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        Page<KadysRealisationDTOResponse> feed = kadysInteractionService
                .listerFeedKadys(email, PageRequest.of(page, size, Sort.by("datePublication").descending()));
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/kadys/{id}")
    public ResponseEntity<KadysRealisationDTOResponse> getRealisation(
            @PathVariable Long id,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(kadysInteractionService.obtenirRealisationKadys(id, email));
    }

    @GetMapping("/kadys/{id}/commentaires")
    public ResponseEntity<List<CommentaireDTOResponse>> getCommentaires(
            @PathVariable Long id,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(kadysInteractionService.listerCommentaires(id, email));
    }

    @PostMapping("/kadys/{id}/vue")
    public ResponseEntity<Void> enregistrerVue(@PathVariable Long id) {
        kadysInteractionService.enregistrerVue(id);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 2. STORIES ACTIVES POUR KADY'S
    // ==========================================

    @GetMapping({"/kadys/stories", "/explore/kadys/stories"})
    public ResponseEntity<List<SalonStoriesDTOResponse>> getStoriesPourKadys() {
        return ResponseEntity.ok(storySalonService.listerStoriesTousSalonsPourKadys());
    }

    // ==========================================
    // 3. INTERACTIONS CLIENT (AUTH REQUISE)
    // ==========================================

    @PostMapping("/kadys/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LikeToggleDTOResponse> toggleLike(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(kadysInteractionService.toggleLike(id, principal.getName()));
    }

    @PostMapping("/kadys/{id}/commentaires")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentaireDTOResponse> ajouterCommentaire(
            @PathVariable Long id,
            @Valid @RequestBody CommentaireCreateDTORequest request,
            Principal principal) {
        CommentaireDTOResponse response = kadysInteractionService.ajouterCommentaire(id, principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/kadys/commentaires/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> supprimerCommentaire(
            @PathVariable Long id,
            Principal principal) {
        kadysInteractionService.supprimerCommentaire(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/inspirations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<KadysRealisationDTOResponse>> getInspirations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Page<KadysRealisationDTOResponse> inspirations = kadysInteractionService
                .listerMesInspirations(principal.getName(), PageRequest.of(page, size));
        return ResponseEntity.ok(inspirations);
    }
}
