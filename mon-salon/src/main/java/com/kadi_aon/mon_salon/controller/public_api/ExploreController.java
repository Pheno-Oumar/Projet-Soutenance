package com.kadi_aon.mon_salon.controller.public_api;

import java.util.List;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse;
import com.kadi_aon.mon_salon.realisation.service.RealisationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.CreneauDisponibleDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.VitrineDisponibiliteDTORequest;
import com.kadi_aon.mon_salon.rendezvous.service.DisponibiliteService;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.service.ExploreSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;
import com.kadi_aon.mon_salon.stock.dto.ProduitDTOResponse;
import com.kadi_aon.mon_salon.stock.service.StockSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/explore")
@RequiredArgsConstructor
@Tag(name = "Exploration Plateforme (Visiteur Public)", description = "Endpoints publics permettant de découvrir et rechercher les salons, services, produits et réalisations de la plateforme")
public class ExploreController {

    private final ExploreSalonService exploreSalonService;
    private final ServiceSalonService serviceSalonService;
    private final StockSalonService stockSalonService;
    private final AvisSalonService avisSalonService;
    private final RealisationSalonService realisationSalonService;
    private final DisponibiliteService disponibiliteService;

    @GetMapping("/salons")
    @Operation(summary = "Lister les salons actifs avec pagination")
    public ResponseEntity<APIResponse<Page<SalonDTOResponse>>> listerSalons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SalonDTOResponse> salons = exploreSalonService.listerSalonsActifs(pageable);
        return ResponseEntity.ok(new APIResponse<>(true, "Liste des salons récupérée avec succès", salons));
    }

    @GetMapping("/salons/search")
    @Operation(summary = "Rechercher des salons par nom, ville ou description")
    public ResponseEntity<APIResponse<List<SalonDTOResponse>>> rechercherSalons(
            @RequestParam(required = false, defaultValue = "") String q) {

        List<SalonDTOResponse> resultats = exploreSalonService.rechercherSalons(q);
        return ResponseEntity.ok(new APIResponse<>(true, "Recherche de salons effectuée", resultats));
    }

    @GetMapping("/salons/nearby")
    @Operation(summary = "Recherche géolocalisée de salons dans un rayon donné (en km) via formule Haversine")
    public ResponseEntity<APIResponse<List<SalonDTOResponse>>> rechercherSalonsProches(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double rayonKm) {

        List<SalonDTOResponse> resultats = exploreSalonService.rechercherSalonsNearby(lat, lng, rayonKm);
        return ResponseEntity.ok(new APIResponse<>(true, "Salons à proximité récupérés avec succès", resultats));
    }

    @GetMapping("/salons/par-service")
    @Operation(summary = "Filtrer les salons proposant un service spécifique")
    public ResponseEntity<APIResponse<List<SalonDTOResponse>>> filtrerParService(
            @RequestParam Long serviceId) {

        List<SalonDTOResponse> resultats = exploreSalonService.rechercherSalonsParService(serviceId);
        return ResponseEntity.ok(new APIResponse<>(true, "Salons filtrés par prestation", resultats));
    }

    @GetMapping("/salons/{slugSalon}")
    @Operation(summary = "Consulter les informations publiques d'un salon")
    public ResponseEntity<APIResponse<SalonDTOResponse>> getSalonDetail(@PathVariable String slugSalon) {
        SalonDTOResponse salon = exploreSalonService.getSalonDetail(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail du salon récupéré", salon));
    }

    @GetMapping("/salons/{slugSalon}/services")
    @Operation(summary = "Consulter le catalogue des services et variantes d'un salon")
    public ResponseEntity<APIResponse<List<ServiceSalonDTOResponse>>> getServicesSalon(@PathVariable String slugSalon) {
        List<ServiceSalonDTOResponse> services = serviceSalonService.listerServices(slugSalon, true);
        return ResponseEntity.ok(new APIResponse<>(true, "Services du salon récupérés avec succès", services));
    }

    @GetMapping("/salons/{slugSalon}/avis")
    @Operation(summary = "Consulter les avis publiés sur un salon")
    public ResponseEntity<APIResponse<List<AvisSalonDTOResponse>>> getAvisSalon(@PathVariable String slugSalon) {
        List<AvisSalonDTOResponse> avis = avisSalonService.listerAvisPubliesSalon(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Avis publiés du salon récupérés", avis));
    }

    @GetMapping("/salons/{slugSalon}/realisations")
    @Operation(summary = "Consulter les réalisations publiées d'un salon spécifique")
    public ResponseEntity<APIResponse<List<RealisationDTOResponse>>> getRealisationsSalon(@PathVariable String slugSalon) {
        List<RealisationDTOResponse> realisations = realisationSalonService.listerRealisationsPubliees(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Réalisations du salon récupérées", realisations));
    }

    @PostMapping("/salons/{slugSalon}/disponibilites")
    @Operation(summary = "Consulter les créneaux disponibles d'un salon pour des variantes choisies (visiteur sans choix de coiffeur)")
    public ResponseEntity<APIResponse<List<CreneauDisponibleDTOResponse>>> getDisponibilitesSalon(
            @PathVariable String slugSalon,
            @Valid @RequestBody VitrineDisponibiliteDTORequest request) {

        List<CreneauDisponibleDTOResponse> creneaux = disponibiliteService.calculerDisponibilites(slugSalon, request.toDisponibiliteSearch());
        return ResponseEntity.ok(new APIResponse<>(true, "Créneaux disponibles récupérés", creneaux));
    }

    @GetMapping("/produits")
    @Operation(summary = "Consulter le catalogue transversal des produits en vente (tous salons confondus)")
    public ResponseEntity<APIResponse<List<ProduitDTOResponse>>> listerProduitsTransversal(
            @RequestParam(required = false) Long categorieId) {

        List<ProduitDTOResponse> produits = stockSalonService.listerProduitsTransversal(categorieId);
        return ResponseEntity.ok(new APIResponse<>(true, "Catalogue des produits récupéré avec succès", produits));
    }

    @GetMapping("/realisations")
    @Operation(summary = "Consulter la galerie globale des réalisations publiées (tous salons confondus)")
    public ResponseEntity<APIResponse<List<RealisationDTOResponse>>> listerToutesRealisations() {
        List<RealisationDTOResponse> realisations = realisationSalonService.listerToutesLesRealisationsPubliees();
        return ResponseEntity.ok(new APIResponse<>(true, "Galerie transversale des réalisations récupérée", realisations));
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @GetMapping("/media/proxy")
    @Operation(summary = "Proxy d'images haute performance (contourne les restrictions de pistage/stockage cross-origin des navigateurs)")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (host == null || (!host.contains("cloudinary.com") && !host.contains("unsplash.com") && !host.contains("images.unsplash.com"))) {
                return ResponseEntity.badRequest().build();
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "HairStyle-Proxy/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String contentType = response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse("image/webp");
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800, immutable")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .body(response.body());
            } else {
                return ResponseEntity.status(response.statusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
