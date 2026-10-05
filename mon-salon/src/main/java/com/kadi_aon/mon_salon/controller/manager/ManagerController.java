package com.kadi_aon.mon_salon.controller.manager;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VariantePrixUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteServiceDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.VarianteUpdateDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.service.CoiffeurSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/manager")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'MANAGER')")
@RequiredArgsConstructor
@Tag(name = "Espace Manager", description = "Gestion des services, des prix et du profil personnel par le manager du salon")
public class ManagerController {

    private final CompteService compteService;
    private final ServiceSalonService serviceSalonService;
    private final com.kadi_aon.mon_salon.avis.service.AvisSalonService avisSalonService;
    private final com.kadi_aon.mon_salon.realisation.service.RealisationSalonService realisationSalonService;
    private final com.kadi_aon.mon_salon.salon.service.ProprietairePilotageService proprietairePilotageService;
    private final com.kadi_aon.mon_salon.salon.service.ReceptionnisteSalonService receptionnisteSalonService;
    private final CoiffeurSalonService coiffeurSalonService;

    // --- GESTION DU COMPTE ---

    @GetMapping("/compte")
    @Operation(summary = "Consulter son profil personnel")
    public ResponseEntity<APIResponse<CompteDTOResponse>> getProfil(
            @PathVariable String slugSalon,
            Principal principal) {

        CompteDTOResponse compte = compteService.getProfil(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Profil récupéré avec succès", compte));
    }

    @PutMapping("/compte")
    @Operation(summary = "Modifier son profil personnel (nom, prénom, date de naissance, téléphone)")
    public ResponseEntity<APIResponse<CompteDTOResponse>> updateProfil(
            @PathVariable String slugSalon,
            @Valid @RequestBody CompteUpdateDTORequest request,
            Principal principal) {

        CompteDTOResponse compte = compteService.updateProfil(principal.getName(), request, slugSalon, "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Profil mis à jour avec succès", compte));
    }

    @PatchMapping("/compte/mot-de-passe")
    @Operation(summary = "Modifier son mot de passe en fournissant l'ancien et le nouveau")
    public ResponseEntity<APIResponse<Void>> changerMotDePasse(
            @PathVariable String slugSalon,
            @Valid @RequestBody ChangementMotDePasseDTORequest request,
            Principal principal) {

        compteService.changerMotDePasse(principal.getName(), request, slugSalon, "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Mot de passe modifié avec succès", null));
    }

    // --- GESTION DES SERVICES ET VARIANTES ---

    @PostMapping(value = "/services", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Créer un service avec ses variantes et image (Multipart)")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> creerService(
            @PathVariable String slugSalon,
            @Valid @org.springframework.web.bind.annotation.RequestPart("data") ServiceSalonCreateDTORequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "image", required = false) MultipartFile image,
            Principal principal) throws java.io.IOException {

        ServiceSalonDTOResponse response = serviceSalonService.creerService(slugSalon, request, image, principal.getName(), "MANAGER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Service créé avec succès", response));
    }

    @GetMapping("/services")
    @Operation(summary = "Lister les services du salon (avec leurs variantes)")
    public ResponseEntity<APIResponse<List<ServiceSalonDTOResponse>>> listerServices(
            @PathVariable String slugSalon,
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {

        List<ServiceSalonDTOResponse> services = serviceSalonService.listerServices(slugSalon, !inclureInactifs);
        return ResponseEntity.ok(new APIResponse<>(true, "Services récupérés avec succès", services));
    }

    @GetMapping("/services/{id}")
    @Operation(summary = "Consulter le détail d'un service")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> getService(
            @PathVariable String slugSalon,
            @PathVariable Long id) {

        ServiceSalonDTOResponse response = serviceSalonService.getService(slugSalon, id);
        return ResponseEntity.ok(new APIResponse<>(true, "Service récupéré avec succès", response));
    }

    @PutMapping(value = "/services/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier le nom et la description d'un service (JSON)")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> modifierService(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody ServiceSalonUpdateDTORequest request,
            Principal principal) {

        ServiceSalonDTOResponse response = serviceSalonService.modifierService(slugSalon, id, request, principal.getName(), "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Service mis à jour avec succès", response));
    }

    @PatchMapping(value = "/services/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour l'image d'un service (supprime l'ancienne sur Cloudinary)")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> uploadImageService(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws java.io.IOException {

        ServiceSalonDTOResponse response = serviceSalonService.uploadImageService(slugSalon, id, file, principal.getName(), "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Image du service mise à jour avec succès", response));
    }

    @PatchMapping("/services/{id}/statut")
    @Operation(summary = "Activer ou désactiver un service")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> basculerStatutService(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        ServiceSalonDTOResponse response = serviceSalonService.basculerStatutService(slugSalon, id, principal.getName(), "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Statut du service mis à jour", response));
    }

    @PostMapping(value = "/services/{id}/variantes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ajouter une variante à un service avec image (Multipart)")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> ajouterVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestPart("data") VarianteCreateDTORequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "image", required = false) MultipartFile image,
            Principal principal) throws java.io.IOException {

        VarianteServiceDTOResponse response = serviceSalonService.ajouterVariante(slugSalon, id, request, image, principal.getName(), "MANAGER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Variante ajoutée avec succès", response));
    }

    @PutMapping(value = "/services/{id}/variantes/{varId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier une variante de service (JSON)")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> modifierVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @PathVariable Long varId,
            @Valid @RequestBody VarianteUpdateDTORequest request,
            Principal principal) {

        VarianteServiceDTOResponse response = serviceSalonService.modifierVariante(slugSalon, id, varId, request, principal.getName(), "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Variante mise à jour avec succès", response));
    }

    @PatchMapping(value = "/services/{id}/variantes/{varId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour l'image d'une variante (supprime l'ancienne sur Cloudinary)")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> uploadImageVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @PathVariable Long varId,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws java.io.IOException {

        VarianteServiceDTOResponse response = serviceSalonService.uploadImageVariante(slugSalon, id, varId, file, principal.getName(), "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Image de la variante mise à jour avec succès", response));
    }

    @PatchMapping("/services/{id}/variantes/{varId}/prix")
    @Operation(summary = "Mettre à jour le prix d'une variante de service")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> modifierPrixVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @PathVariable Long varId,
            @Valid @RequestBody VariantePrixUpdateDTORequest request,
            Principal principal) {

        VarianteServiceDTOResponse response = serviceSalonService.modifierPrixVariante(slugSalon, id, varId, request, principal.getName(), "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Prix de la variante mis à jour", response));
    }

    @PatchMapping("/services/{id}/variantes/{varId}/statut")
    @Operation(summary = "Activer ou désactiver une variante de service")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> basculerStatutVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @PathVariable Long varId,
            Principal principal) {

        VarianteServiceDTOResponse response = serviceSalonService.basculerStatutVariante(slugSalon, id, varId, principal.getName(), "MANAGER");
        return ResponseEntity.ok(new APIResponse<>(true, "Statut de la variante mis à jour", response));
    }

    // --- MODÉRATION DES AVIS ---

    @GetMapping("/avis/prestations")
    @Operation(summary = "Lister les avis sur les prestations du salon (avec filtre facultatif par statut)")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse>>> listerAvisPrestations(
            @PathVariable String slugSalon,
            @RequestParam(required = false) Boolean statut,
            Principal principal) {

        List<com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse> list =
                avisSalonService.listerAvisPrestationsPourManager(slugSalon, principal.getName(), statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Avis prestations récupérés", list));
    }

    @PatchMapping("/avis/prestations/{id}/statut")
    @Operation(summary = "Modérer le statut d'un avis sur prestation (valider ou rejeter, sans altérer le contenu)")
    public ResponseEntity<APIResponse<com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse>> modererAvisPrestation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody com.kadi_aon.mon_salon.avis.dto.AvisModerationDTORequest request,
            Principal principal) {

        com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse response =
                avisSalonService.modererAvisPrestation(slugSalon, id, principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Statut de l'avis prestation mis à jour", response));
    }

    @GetMapping("/avis/salon")
    @Operation(summary = "Lister les avis sur le salon (avec filtre facultatif par statut)")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse>>> listerAvisSalon(
            @PathVariable String slugSalon,
            @RequestParam(required = false) Boolean statut,
            Principal principal) {

        List<com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse> list =
                avisSalonService.listerAvisSalonPourManager(slugSalon, principal.getName(), statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Avis sur le salon récupérés", list));
    }

    @PatchMapping("/avis/salon/{id}/statut")
    @Operation(summary = "Modérer le statut d'un avis sur salon (valider ou rejeter, sans altérer le contenu)")
    public ResponseEntity<APIResponse<com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse>> modererAvisSalon(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody com.kadi_aon.mon_salon.avis.dto.AvisModerationDTORequest request,
            Principal principal) {

        com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse response =
                avisSalonService.modererAvisSalon(slugSalon, id, principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Statut de l'avis salon mis à jour", response));
    }

    // --- GESTION DES RÉALISATIONS ---

    @PostMapping(path = "/realisations", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Créer une réalisation avec vidéo obligatoire (upload automatique sur Cloudinary)")
    public ResponseEntity<APIResponse<com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse>> creerRealisation(
            @PathVariable String slugSalon,
            @Valid @org.springframework.web.bind.annotation.RequestPart("data") com.kadi_aon.mon_salon.realisation.dto.RealisationCreateDTORequest request,
            @org.springframework.web.bind.annotation.RequestPart("video") org.springframework.web.multipart.MultipartFile video,
            Principal principal) throws java.io.IOException {

        com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse response =
                realisationSalonService.creerRealisation(slugSalon, principal.getName(), request, video);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Réalisation créée avec succès", response));
    }

    @PutMapping(path = "/realisations/{id}", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier les données d'une réalisation (titre, description, date, coiffeur)")
    public ResponseEntity<APIResponse<com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse>> modifierRealisation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody com.kadi_aon.mon_salon.realisation.dto.RealisationUpdateDTORequest request,
            Principal principal) throws java.io.IOException {

        com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse response =
                realisationSalonService.modifierRealisation(slugSalon, id, principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Réalisation mise à jour avec succès", response));
    }

    @PatchMapping(path = "/realisations/{id}/video", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour la vidéo d'une réalisation (supprime l'ancienne sur Cloudinary)")
    public ResponseEntity<APIResponse<com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse>> uploadVideoRealisation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Principal principal) throws java.io.IOException {

        com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse response =
                realisationSalonService.uploadVideoRealisation(slugSalon, id, principal.getName(), file);
        return ResponseEntity.ok(new APIResponse<>(true, "Vidéo de la réalisation mise à jour avec succès", response));
    }

    @PatchMapping("/realisations/{id}/publication")
    @Operation(summary = "Modifier le statut de publication d'une réalisation")
    public ResponseEntity<APIResponse<com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse>> modifierStatutPublication(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody com.kadi_aon.mon_salon.realisation.dto.RealisationPublicationDTORequest request,
            Principal principal) {

        com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse response =
                realisationSalonService.modifierStatutPublication(slugSalon, id, principal.getName(), request.statutPublication());
        return ResponseEntity.ok(new APIResponse<>(true, "Statut de publication mis à jour", response));
    }

    @GetMapping("/realisations/{id}")
    @Operation(summary = "Consulter le détail d'une réalisation du salon (pour le manager)")
    public ResponseEntity<APIResponse<com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse>> obtenirRealisation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse response =
                realisationSalonService.obtenirRealisationPourManager(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Réalisation récupérée", response));
    }

    @GetMapping("/realisations")
    @Operation(summary = "Lister toutes les réalisations du salon (avec filtre facultatif par statutPublication)")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse>>> listerRealisations(
            @PathVariable String slugSalon,
            @RequestParam(required = false) Boolean statutPublication,
            Principal principal) {

        List<com.kadi_aon.mon_salon.realisation.dto.RealisationDTOResponse> list =
                realisationSalonService.listerRealisationsPourManager(slugSalon, principal.getName(), statutPublication);
        return ResponseEntity.ok(new APIResponse<>(true, "Réalisations récupérées", list));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/realisations/{id}")
    @Operation(summary = "Supprimer une réalisation (supprime également la vidéo sur Cloudinary)")
    public ResponseEntity<APIResponse<Void>> supprimerRealisation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        realisationSalonService.supprimerRealisation(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Réalisation supprimée avec succès", null));
    }

    // --- CLIENTS DU SALON ---

    @GetMapping("/clients")
    @Operation(summary = "Lister les clients enregistrés dans le salon")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse>>> listerClients(
            @PathVariable String slugSalon) {

        List<com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse> clients =
                proprietairePilotageService.listerClientsDuSalon(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Clients du salon récupérés avec succès", clients));
    }

    @GetMapping("/clients/{clientId}")
    @Operation(summary = "Consulter le dossier d'un client sans plan financier (RDVs, prestations, profil capillaire)")
    public ResponseEntity<APIResponse<com.kadi_aon.mon_salon.salon.dto.pilotage.FicheClientManagerDTOResponse>> getClientDetail(
            @PathVariable String slugSalon,
            @PathVariable Long clientId) {

        com.kadi_aon.mon_salon.salon.dto.pilotage.FicheClientManagerDTOResponse fiche =
                proprietairePilotageService.obtenirFicheClientPourManager(slugSalon, clientId);
        return ResponseEntity.ok(new APIResponse<>(true, "Dossier client récupéré avec succès", fiche));
    }

    // --- PLANNING DU SALON ---

    @GetMapping("/planning")
    @Operation(summary = "Consulter le planning du salon (avec filtre optionnel par date et coiffeur)")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse>>> getPlanning(
            @PathVariable String slugSalon,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate date,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long coiffeurAffectationId) {

        List<com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse> planning =
                receptionnisteSalonService.consulterPlanning(slugSalon, date, coiffeurAffectationId);
        return ResponseEntity.ok(new APIResponse<>(true, "Planning du salon récupéré avec succès", planning));
    }

    // --- GESTION DES INDISPONIBILITÉS COIFFEURS ---

    @PostMapping("/indisponibilites")
    @Operation(summary = "Créer une indisponibilité pour un coiffeur")
    public ResponseEntity<APIResponse<IndisponibiliteDTOResponse>> creerIndisponibilite(
            @PathVariable String slugSalon,
            @Valid @RequestBody IndisponibiliteDTORequest request,
            Principal principal) {

        IndisponibiliteDTOResponse response = coiffeurSalonService.ajouterIndisponibiliteParManagerOuProprio(
                slugSalon, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Indisponibilité créée avec succès", response));
    }

    @PutMapping("/indisponibilites/{id}")
    @Operation(summary = "Modifier une indisponibilité (uniquement avant sa date de début)")
    public ResponseEntity<APIResponse<IndisponibiliteDTOResponse>> modifierIndisponibilite(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody IndisponibiliteDTORequest request,
            Principal principal) {

        IndisponibiliteDTOResponse response = coiffeurSalonService.modifierIndisponibiliteParManagerOuProprio(
                slugSalon, id, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Indisponibilité modifiée avec succès", response));
    }

    @DeleteMapping("/indisponibilites/{id}")
    @Operation(summary = "Supprimer une indisponibilité (uniquement avant sa date de début)")
    public ResponseEntity<APIResponse<Void>> supprimerIndisponibilite(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        coiffeurSalonService.supprimerIndisponibiliteParManagerOuProprio(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Indisponibilité supprimée avec succès", null));
    }

    @GetMapping("/indisponibilites")
    @Operation(summary = "Lister les indisponibilités des coiffeurs du salon")
    public ResponseEntity<APIResponse<List<IndisponibiliteDTOResponse>>> listerIndisponibilites(
            @PathVariable String slugSalon,
            @RequestParam(required = false) Long coiffeurAffectationId,
            Principal principal) {

        List<IndisponibiliteDTOResponse> list = coiffeurSalonService.listerIndisponibilitesSalon(
                slugSalon, coiffeurAffectationId, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Indisponibilités récupérées avec succès", list));
    }

    // --- COIFFEURS DU SALON ---

    @GetMapping("/coiffeurs")
    @Operation(summary = "Lister les coiffeurs du salon pour les formulaires de sélection et le planning")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse>>> listerCoiffeurs(
            @PathVariable String slugSalon) {
        List<com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse> list =
                coiffeurSalonService.listerCoiffeursVitrine(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Coiffeurs du salon récupérés avec succès", list));
    }
}


