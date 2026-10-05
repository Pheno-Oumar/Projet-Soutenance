package com.kadi_aon.mon_salon.controller.client;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.avis.dto.AvisPrestationCreateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationUpdateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonCreateDTORequest;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.favori.dto.FavoriCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.favori.dto.FavoriSalonDTOResponse;
import com.kadi_aon.mon_salon.favori.service.FavoriSalonService;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousCreateDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.service.RendezVousService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/client")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'CLIENT')")
@RequiredArgsConstructor
@Tag(name = "Client Salon (Dédié)", description = "Endpoints réservés aux actions d'un client au sein d'un salon spécifique (Rendez-vous, Prestations, Paiements, Avis et Favoris du salon)")
public class ClientController {

    private final RendezVousService rendezVousService;
    private final PrestationSalonService prestationSalonService;
    private final FacturationSalonService facturationSalonService;
    private final AvisSalonService avisSalonService;
    private final FavoriSalonService favoriSalonService;

    // ==========================================
    // 1. RENDEZ-VOUS DANS CE SALON
    // ==========================================

    @PostMapping("/rendez-vous")
    @Operation(summary = "Réserver un rendez-vous dans ce salon")
    public ResponseEntity<APIResponse<RendezVousDTOResponse>> reserverRendezVous(
            @PathVariable String slugSalon,
            @Valid @RequestBody RendezVousCreateDTORequest request,
            Principal principal) {

        RendezVousDTOResponse response = rendezVousService.creerRendezVousClient(slugSalon, request,
                principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Rendez-vous confirmé avec succès", response));
    }

    @GetMapping("/rendez-vous")
    @Operation(summary = "Consulter ses rendez-vous dans ce salon")
    public ResponseEntity<APIResponse<List<RendezVousDTOResponse>>> listerMesRendezVous(
            @PathVariable String slugSalon,
            Principal principal) {

        List<RendezVousDTOResponse> response = rendezVousService.listerMesRendezVous(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Liste de vos rendez-vous dans ce salon récupérée", response));
    }

    @GetMapping("/rendez-vous/{id}")
    @Operation(summary = "Consulter le détail d'un de ses rendez-vous dans ce salon")
    public ResponseEntity<APIResponse<RendezVousDTOResponse>> getDetailRendezVous(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        RendezVousDTOResponse response = rendezVousService.getDetailRendezVous(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Détail du rendez-vous récupéré", response));
    }

    @PatchMapping("/rendez-vous/{id}/annuler")
    @Operation(summary = "Annuler un de ses rendez-vous (libère immédiatement le créneau)")
    public ResponseEntity<APIResponse<RendezVousDTOResponse>> annulerRendezVous(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody RendezVousAnnulationDTORequest request,
            Principal principal) {

        RendezVousDTOResponse response = rendezVousService.annulerRendezVousClient(slugSalon, id, request,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Rendez-vous annulé avec succès", response));
    }


    // ==========================================
    // 2. PRESTATIONS DANS CE SALON
    // ==========================================

    @GetMapping("/prestations")
    @Operation(summary = "Consulter l'historique de ses prestations passées dans ce salon")
    public ResponseEntity<APIResponse<List<PrestationDTOResponse>>> listerMesPrestations(
            @PathVariable String slugSalon,
            Principal principal) {

        List<PrestationDTOResponse> response = prestationSalonService.listerPrestationsClientSalon(slugSalon,
                principal.getName());
        return ResponseEntity
                .ok(new APIResponse<>(true, "Historique de vos prestations dans ce salon récupéré", response));
    }

    @GetMapping("/prestations/{id}")
    @Operation(summary = "Consulter le détail d'une prestation dans ce salon")
    public ResponseEntity<APIResponse<PrestationDTOResponse>> getDetailPrestation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        PrestationDTOResponse response = prestationSalonService.obtenirDetailPrestationClient(id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la prestation récupéré", response));
    }

    // ==========================================
    // 3. PAIEMENTS DANS CE SALON
    // ==========================================

    @GetMapping("/paiements")
    @Operation(summary = "Consulter la liste de ses paiements dans ce salon")
    public ResponseEntity<APIResponse<List<PaiementDTOResponse>>> listerMesPaiements(
            @PathVariable String slugSalon,
            Principal principal) {

        List<PaiementDTOResponse> response = facturationSalonService.listerPaiementsClient(slugSalon,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Liste de vos paiements dans ce salon récupérée", response));
    }

    @GetMapping("/paiements/{id}")
    @Operation(summary = "Consulter le détail d'un de ses paiements dans ce salon")
    public ResponseEntity<APIResponse<PaiementDTOResponse>> getDetailPaiement(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        PaiementDTOResponse response = facturationSalonService.obtenirDetailPaiementClient(slugSalon, id,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Détail du paiement récupéré", response));
    }

    // ==========================================
    // 4. AVIS SUR PRESTATION DANS CE SALON
    // ==========================================

    @PostMapping("/avis/prestations")
    @Operation(summary = "Déposer un avis noté sur une prestation terminée de ce salon")
    public ResponseEntity<APIResponse<AvisPrestationDTOResponse>> creerAvisPrestation(
            @PathVariable String slugSalon,
            @Valid @RequestBody AvisPrestationCreateDTORequest request,
            Principal principal) {

        AvisPrestationDTOResponse response = avisSalonService.creerAvisPrestation(slugSalon, principal.getName(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Avis enregistré (en attente de modération)", response));
    }

    @PutMapping("/avis/prestations/{id}")
    @Operation(summary = "Modifier son avis sur une prestation")
    public ResponseEntity<APIResponse<AvisPrestationDTOResponse>> modifierAvisPrestation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody AvisPrestationUpdateDTORequest request,
            Principal principal) {

        AvisPrestationDTOResponse response = avisSalonService.modifierAvisPrestation(slugSalon, id, principal.getName(),
                request);
        return ResponseEntity.ok(new APIResponse<>(true, "Avis mis à jour (en attente de modération)", response));
    }

    @DeleteMapping("/avis/prestations/{id}")
    @Operation(summary = "Supprimer son avis sur une prestation")
    public ResponseEntity<APIResponse<Void>> supprimerAvisPrestation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        avisSalonService.supprimerAvisPrestation(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Avis prestation supprimé avec succès", null));
    }

    @GetMapping("/avis/prestations/mes-avis")
    @Operation(summary = "Consulter ses avis déposés sur les prestations dans ce salon")
    public ResponseEntity<APIResponse<List<AvisPrestationDTOResponse>>> listerMesAvisPrestations(
            @PathVariable String slugSalon,
            Principal principal) {

        List<AvisPrestationDTOResponse> list = avisSalonService.listerMesAvisPrestations(slugSalon,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Vos avis prestations récupérés", list));
    }

    @GetMapping("/avis/prestations/ligne/{ligneId}")
    @Operation(summary = "Consulter les avis validés pour une ligne de prestation donnée")
    public ResponseEntity<APIResponse<List<AvisPrestationDTOResponse>>> listerAvisLignePrestation(
            @PathVariable String slugSalon,
            @PathVariable Long ligneId) {

        List<AvisPrestationDTOResponse> list = avisSalonService.listerAvisValidesLignePrestation(slugSalon, ligneId);
        return ResponseEntity.ok(new APIResponse<>(true, "Avis validés récupérés", list));
    }

    // ==========================================
    // 5. AVIS SUR CE SALON
    // ==========================================

    @PostMapping("/avis/salon")
    @Operation(summary = "Déposer un avis sur ce salon (après au moins une prestation)")
    public ResponseEntity<APIResponse<AvisSalonDTOResponse>> creerAvisSalon(
            @PathVariable String slugSalon,
            @Valid @RequestBody AvisSalonCreateDTORequest request,
            Principal principal) {

        AvisSalonDTOResponse response = avisSalonService.creerAvisSalon(slugSalon, principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Avis sur le salon enregistré (en attente de modération)", response));
    }

    @PutMapping("/avis/salon")
    @Operation(summary = "Modifier son avis sur ce salon")
    public ResponseEntity<APIResponse<AvisSalonDTOResponse>> modifierAvisSalon(
            @PathVariable String slugSalon,
            @Valid @RequestBody AvisSalonUpdateDTORequest request,
            Principal principal) {

        AvisSalonDTOResponse response = avisSalonService.modifierAvisSalon(slugSalon, principal.getName(), request);
        return ResponseEntity
                .ok(new APIResponse<>(true, "Avis sur le salon mis à jour (en attente de modération)", response));
    }

    @DeleteMapping({"/avis/salon", "/avis/salon/{id}"})
    @Operation(summary = "Supprimer son avis sur ce salon")
    public ResponseEntity<APIResponse<Void>> supprimerAvisSalon(
            @PathVariable String slugSalon,
            Principal principal) {

        avisSalonService.supprimerAvisSalon(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Avis sur le salon supprimé avec succès", null));
    }

    @GetMapping("/avis/salon/mon-avis")
    @Operation(summary = "Consulter son propre avis sur ce salon")
    public ResponseEntity<APIResponse<AvisSalonDTOResponse>> getMonAvisSalon(
            @PathVariable String slugSalon,
            Principal principal) {

        AvisSalonDTOResponse response = avisSalonService.obtenirMonAvisSalon(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Votre avis sur le salon récupéré", response));
    }

    // ==========================================
    // 6. FAVORIS LIÉS À CE SALON
    // ==========================================

    @PostMapping("/favoris/salon")
    @Operation(summary = "Ajouter ce salon à ses favoris")
    public ResponseEntity<APIResponse<FavoriSalonDTOResponse>> ajouterSalonFavori(
            @PathVariable String slugSalon,
            Principal principal) {

        FavoriSalonDTOResponse response = favoriSalonService.ajouterSalonFavori(slugSalon, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Salon ajouté à vos favoris", response));
    }

    @DeleteMapping("/favoris/salon")
    @Operation(summary = "Retirer ce salon de ses favoris")
    public ResponseEntity<APIResponse<Void>> retirerSalonFavori(
            @PathVariable String slugSalon,
            Principal principal) {

        favoriSalonService.retirerSalonFavori(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Salon retiré de vos favoris", null));
    }

    @GetMapping("/favoris/salon/status")
    @Operation(summary = "Vérifier si ce salon est dans ses favoris")
    public ResponseEntity<APIResponse<Boolean>> isSalonFavori(
            @PathVariable String slugSalon,
            Principal principal) {

        boolean estFavori = favoriSalonService.isSalonFavori(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Statut favori vérifié", estFavori));
    }

    @PostMapping("/favoris/coiffeurs/{coiffeurId}")
    @Operation(summary = "Ajouter un coiffeur de ce salon à ses favoris")
    public ResponseEntity<APIResponse<FavoriCoiffeurDTOResponse>> ajouterCoiffeurFavori(
            @PathVariable String slugSalon,
            @PathVariable Long coiffeurId,
            Principal principal) {

        FavoriCoiffeurDTOResponse response = favoriSalonService.ajouterCoiffeurFavori(slugSalon, coiffeurId,
                principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Coiffeur ajouté à vos favoris", response));
    }

    @DeleteMapping("/favoris/coiffeurs/{coiffeurId}")
    @Operation(summary = "Retirer un coiffeur de ce salon de ses favoris")
    public ResponseEntity<APIResponse<Void>> retirerCoiffeurFavori(
            @PathVariable String slugSalon,
            @PathVariable Long coiffeurId,
            Principal principal) {

        favoriSalonService.retirerCoiffeurFavori(slugSalon, coiffeurId, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Coiffeur retiré de vos favoris", null));
    }

    @GetMapping("/favoris/coiffeurs")
    @Operation(summary = "Lister ses coiffeurs favoris pour ce salon")
    public ResponseEntity<APIResponse<List<FavoriCoiffeurDTOResponse>>> listerMesCoiffeursFavoris(
            @PathVariable String slugSalon,
            Principal principal) {

        List<FavoriCoiffeurDTOResponse> list = favoriSalonService.listerMesCoiffeursFavoris(slugSalon,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Vos coiffeurs favoris pour ce salon récupérés", list));
    }
}
