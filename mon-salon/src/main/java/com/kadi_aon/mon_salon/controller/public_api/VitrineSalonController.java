package com.kadi_aon.mon_salon.controller.public_api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.service.RealisationSalonService;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.service.CoiffeurSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.CreneauDisponibleDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.VitrineDisponibiliteDTORequest;
import com.kadi_aon.mon_salon.rendezvous.service.DisponibiliteService;
import com.kadi_aon.mon_salon.salon.dto.ClientRegisterDTORequest;
import com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.VitrineRendezVousDTORequest;
import com.kadi_aon.mon_salon.salon.service.ClientSalonService;
import com.kadi_aon.mon_salon.salon.service.ExploreSalonService;
import com.kadi_aon.mon_salon.salon.service.HoraireSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;
import com.kadi_aon.mon_salon.stock.dto.CategorieProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.VitrineCommandeDTORequest;
import com.kadi_aon.mon_salon.stock.service.CommandeSalonService;
import com.kadi_aon.mon_salon.stock.service.StockSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/vitrine")
@RequiredArgsConstructor
@Tag(name = "Vitrine Salon (Visiteur Public)", description = "Endpoints publics de l'espace vitrine d'un salon spécifique (catalogue, avis, réalisations, disponibilités et inscription client)")
public class VitrineSalonController {

    private final ExploreSalonService exploreSalonService;
    private final ServiceSalonService serviceSalonService;
    private final StockSalonService stockSalonService;
    private final CommandeSalonService commandeSalonService;
    private final AvisSalonService avisSalonService;
    private final RealisationSalonService realisationSalonService;
    private final DisponibiliteService disponibiliteService;
    private final ClientSalonService clientSalonService;
    private final CoiffeurSalonService coiffeurSalonService;
    private final com.kadi_aon.mon_salon.story.service.StorySalonService storySalonService;
    private final HoraireSalonService horaireSalonService;

    @GetMapping("/infos")
    @Operation(summary = "Consulter les informations et coordonnées de la vitrine du salon")
    public ResponseEntity<APIResponse<SalonDTOResponse>> getInfosSalon(@PathVariable String slugSalon) {
        SalonDTOResponse response = exploreSalonService.getSalonDetail(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Informations de la vitrine récupérées", response));
    }

    @GetMapping("/services")
    @Operation(summary = "Consulter le catalogue des prestations et variantes du salon")
    public ResponseEntity<APIResponse<List<ServiceSalonDTOResponse>>> getServices(@PathVariable String slugSalon) {
        List<ServiceSalonDTOResponse> services = serviceSalonService.listerServices(slugSalon, true);
        return ResponseEntity.ok(new APIResponse<>(true, "Catalogue des prestations du salon", services));
    }

    @GetMapping("/categories-produits")
    @Operation(summary = "Consulter les catégories de produits en vente dans ce salon")
    public ResponseEntity<APIResponse<List<CategorieProduitDTOResponse>>> getCategoriesProduits(@PathVariable String slugSalon) {
        List<CategorieProduitDTOResponse> categories = stockSalonService.listerCategories(slugSalon, true);
        return ResponseEntity.ok(new APIResponse<>(true, "Catégories de produits du salon", categories));
    }

    @GetMapping("/produits")
    @Operation(summary = "Consulter les produits en vente dans ce salon (avec filtre catégorie optionnel)")
    public ResponseEntity<APIResponse<List<ProduitDTOResponse>>> getProduits(
            @PathVariable String slugSalon,
            @RequestParam(required = false) Long categorieId) {

        List<ProduitDTOResponse> produits = stockSalonService.listerProduits(slugSalon, categorieId, true);
        return ResponseEntity.ok(new APIResponse<>(true, "Produits disponibles dans le salon", produits));
    }

    @GetMapping("/coiffeurs")
    @Operation(summary = "Consulter l'équipe des coiffeurs du salon et leurs profils publics")
    public ResponseEntity<APIResponse<List<ProfilCoiffeurDTOResponse>>> getCoiffeurs(@PathVariable String slugSalon) {
        List<ProfilCoiffeurDTOResponse> coiffeurs = coiffeurSalonService.listerCoiffeursVitrine(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Équipe des coiffeurs du salon récupérée", coiffeurs));
    }

    @PostMapping("/disponibilites")
    @Operation(summary = "Consulter les créneaux disponibles du salon selon les prestations sélectionnées (visiteur sans choix de coiffeur)")
    public ResponseEntity<APIResponse<List<CreneauDisponibleDTOResponse>>> getDisponibilites(
            @PathVariable String slugSalon,
            @Valid @RequestBody VitrineDisponibiliteDTORequest request) {

        List<CreneauDisponibleDTOResponse> creneaux = disponibiliteService.calculerDisponibilites(slugSalon, request.toDisponibiliteSearch());
        return ResponseEntity.ok(new APIResponse<>(true, "Créneaux disponibles du salon", creneaux));
    }

    @GetMapping("/avis")
    @Operation(summary = "Consulter les avis clients validés et publiés sur ce salon")
    public ResponseEntity<APIResponse<List<AvisSalonDTOResponse>>> getAvis(@PathVariable String slugSalon) {
        List<AvisSalonDTOResponse> avis = avisSalonService.listerAvisPubliesSalon(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Avis vérifiés du salon", avis));
    }

    @GetMapping("/realisations")
    @Operation(summary = "Consulter la galerie photo/vidéo des réalisations de ce salon")
    public ResponseEntity<APIResponse<List<RealisationDTOResponse>>> getRealisations(@PathVariable String slugSalon) {
        List<RealisationDTOResponse> realisations = realisationSalonService.listerRealisationsPubliees(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Galerie des réalisations du salon", realisations));
    }

    @PostMapping({"/register", "/client/register"})
    @Operation(summary = "Créer un compte client et l'enregistrer dans ce salon")
    public ResponseEntity<APIResponse<CompteDTOResponse>> register(
            @PathVariable String slugSalon,
            @Valid @RequestBody ClientRegisterDTORequest request) {

        CompteDTOResponse response = clientSalonService.enregistrerClientDansSalon(slugSalon, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Compte client créé avec succès dans le salon", response));
    }

    @PostMapping("/rendez-vous")
    @Operation(summary = "Réserver un rendez-vous en ligne depuis la vitrine (visiteur ou client)")
    public ResponseEntity<APIResponse<RendezVousDTOResponse>> reserverRendezVousVitrine(
            @PathVariable String slugSalon,
            @Valid @RequestBody VitrineRendezVousDTORequest request) {

        RendezVousDTOResponse response = clientSalonService.reserverRendezVousDepuisVitrine(slugSalon, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Rendez-vous confirmé et enregistré avec succès", response));
    }

    @PostMapping("/commandes")
    @Operation(summary = "Passer une commande Click & Collect de produits depuis la vitrine (visiteur ou client)")
    public ResponseEntity<APIResponse<CommandeDTOResponse>> passerCommandeVitrine(
            @PathVariable String slugSalon,
            @Valid @RequestBody VitrineCommandeDTORequest request) {

        CommandeDTOResponse response = commandeSalonService.passerCommandeDepuisVitrine(slugSalon, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Commande Click & Collect enregistrée avec succès", response));
    }

    @GetMapping("/stories")
    @Operation(summary = "Consulter les stories actives (< 24h) de ce salon")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.story.dto.StoryDTOResponse>>> getStories(@PathVariable String slugSalon) {
        List<com.kadi_aon.mon_salon.story.dto.StoryDTOResponse> stories = storySalonService.listerStoriesSalon(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Stories actives du salon", stories));
    }

    @GetMapping("/horaires")
    @Operation(summary = "Consulter les horaires d'ouverture du salon")
    public ResponseEntity<APIResponse<List<HoraireOuvertureDTOResponse>>> getHoraires(@PathVariable String slugSalon) {
        List<HoraireOuvertureDTOResponse> horaires = horaireSalonService.listerHoraires(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Horaires d'ouverture du salon", horaires));
    }
}
