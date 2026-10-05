package com.kadi_aon.mon_salon.controller.proprietaire;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.salon.dto.EmployeCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.EmployeDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.EmployeUpdateRolesDTORequest;
import com.kadi_aon.mon_salon.salon.dto.FermetureExceptionnelleDTORequest;
import com.kadi_aon.mon_salon.salon.dto.FermetureExceptionnelleDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTORequest;
import com.kadi_aon.mon_salon.salon.dto.HoraireOuvertureDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.SalonUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ServiceSalonUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteCreateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VariantePrixUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.VarianteServiceDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.VarianteUpdateDTORequest;
import com.kadi_aon.mon_salon.salon.dto.TransfertProprieteDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.service.CoiffeurSalonService;
import com.kadi_aon.mon_salon.salon.service.EmployeSalonService;
import com.kadi_aon.mon_salon.salon.service.HoraireSalonService;
import com.kadi_aon.mon_salon.salon.service.ProprietaireSalonService;
import com.kadi_aon.mon_salon.salon.service.ServiceSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/proprietaire")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'PROPRIETAIRE')")
@RequiredArgsConstructor
@Tag(name = "Espace Propriétaire", description = "Gestion du salon, des employés, horaires, services et compte par le propriétaire")
public class ProprietaireController {

    private final ProprietaireSalonService proprietaireSalonService;
    private final CompteService compteService;
    private final EmployeSalonService employeSalonService;
    private final HoraireSalonService horaireSalonService;
    private final ServiceSalonService serviceSalonService;
    private final CoiffeurSalonService coiffeurSalonService;
    private final com.kadi_aon.mon_salon.salon.service.ReceptionnisteSalonService receptionnisteSalonService;

    // --- GESTION DU SALON ---

    @GetMapping("/salon")
    @Operation(summary = "Consulter les informations du salon")
    public ResponseEntity<APIResponse<SalonDTOResponse>> getSalon(
            @PathVariable String slugSalon,
            Principal principal) {

        SalonDTOResponse salon = proprietaireSalonService.getSalon(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Informations du salon récupérées", salon));
    }

    @PutMapping("/salon")
    @Operation(summary = "Modifier les informations du salon")
    public ResponseEntity<APIResponse<SalonDTOResponse>> updateSalon(
            @PathVariable String slugSalon,
            @RequestBody SalonUpdateDTORequest request,
            Principal principal) {

        SalonDTOResponse salon = proprietaireSalonService.updateSalon(slugSalon, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Salon mis à jour avec succès", salon));
    }

    @PatchMapping(path = "/salon/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader le logo du salon (converti automatiquement en format WebP sur Cloudinary)")
    public ResponseEntity<APIResponse<SalonDTOResponse>> uploadLogo(
            @PathVariable String slugSalon,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {

        SalonDTOResponse salon = proprietaireSalonService.uploadLogo(slugSalon, file, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Logo mis à jour avec succès (format WebP)", salon));
    }

    // --- GESTION DES EMPLOYÉS ---

    @PostMapping("/employes")
    @Operation(summary = "Recruter / créer un employé dans le salon")
    public ResponseEntity<APIResponse<EmployeDTOResponse>> creerEmploye(
            @PathVariable String slugSalon,
            @Valid @RequestBody EmployeCreateDTORequest request,
            Principal principal) {

        EmployeDTOResponse response = employeSalonService.creerEmploye(slugSalon, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Employé créé avec succès", response));
    }

    @GetMapping("/employes")
    @Operation(summary = "Lister les employés du salon")
    public ResponseEntity<APIResponse<List<EmployeDTOResponse>>> listerEmployes(
            @PathVariable String slugSalon) {

        List<EmployeDTOResponse> employes = employeSalonService.listerEmployes(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Liste des employés récupérée", employes));
    }

    @GetMapping("/employes/{affectationId}")
    @Operation(summary = "Consulter le détail d'un employé")
    public ResponseEntity<APIResponse<EmployeDTOResponse>> getEmploye(
            @PathVariable String slugSalon,
            @PathVariable Long affectationId) {

        EmployeDTOResponse response = employeSalonService.getEmploye(slugSalon, affectationId);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de l'employé récupéré", response));
    }

    @PutMapping("/employes/{affectationId}/roles")
    @Operation(summary = "Modifier les rôles d'un employé")
    public ResponseEntity<APIResponse<EmployeDTOResponse>> updateRoles(
            @PathVariable String slugSalon,
            @PathVariable Long affectationId,
            @Valid @RequestBody EmployeUpdateRolesDTORequest request,
            Principal principal) {

        EmployeDTOResponse response = employeSalonService.updateRoles(slugSalon, affectationId, request,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Rôles de l'employé mis à jour", response));
    }

    @PatchMapping("/employes/{affectationId}/desactiver")
    @Operation(summary = "Désactiver un employé")
    public ResponseEntity<APIResponse<EmployeDTOResponse>> desactiverEmploye(
            @PathVariable String slugSalon,
            @PathVariable Long affectationId,
            Principal principal) {

        EmployeDTOResponse response = employeSalonService.desactiverEmploye(slugSalon, affectationId,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Employé désactivé avec succès", response));
    }

    @PatchMapping("/employes/{affectationId}/reactiver")
    @Operation(summary = "Réactiver un employé")
    public ResponseEntity<APIResponse<EmployeDTOResponse>> reactiverEmploye(
            @PathVariable String slugSalon,
            @PathVariable Long affectationId,
            Principal principal) {

        EmployeDTOResponse response = employeSalonService.reactiverEmploye(slugSalon, affectationId,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Employé réactivé avec succès", response));
    }

    // --- GESTION DES HORAIRES ET FERMETURES ---

    @PutMapping("/horaires")
    @Operation(summary = "Définir ou mettre à jour un horaire d'ouverture pour un jour de la semaine")
    public ResponseEntity<APIResponse<HoraireOuvertureDTOResponse>> definirHoraire(
            @PathVariable String slugSalon,
            @Valid @RequestBody HoraireOuvertureDTORequest request,
            Principal principal) {

        HoraireOuvertureDTOResponse response = horaireSalonService.definirHoraire(slugSalon, request,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Horaire enregistré avec succès", response));
    }

    @GetMapping("/horaires")
    @Operation(summary = "Lister tous les horaires d'ouverture du salon")
    public ResponseEntity<APIResponse<List<HoraireOuvertureDTOResponse>>> listerHoraires(
            @PathVariable String slugSalon) {

        List<HoraireOuvertureDTOResponse> horaires = horaireSalonService.listerHoraires(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Horaires d'ouverture récupérés", horaires));
    }

    @PostMapping("/fermetures")
    @Operation(summary = "Ajouter une fermeture exceptionnelle")
    public ResponseEntity<APIResponse<FermetureExceptionnelleDTOResponse>> ajouterFermeture(
            @PathVariable String slugSalon,
            @Valid @RequestBody FermetureExceptionnelleDTORequest request,
            Principal principal) {

        FermetureExceptionnelleDTOResponse response = horaireSalonService.ajouterFermeture(slugSalon, request,
                principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Fermeture exceptionnelle enregistrée avec succès", response));
    }

    @GetMapping("/fermetures")
    @Operation(summary = "Lister les fermetures exceptionnelles du salon")
    public ResponseEntity<APIResponse<List<FermetureExceptionnelleDTOResponse>>> listerFermetures(
            @PathVariable String slugSalon) {

        List<FermetureExceptionnelleDTOResponse> fermetures = horaireSalonService.listerFermetures(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Fermetures exceptionnelles récupérées", fermetures));
    }

    @DeleteMapping("/fermetures/{id}")
    @Operation(summary = "Supprimer une fermeture exceptionnelle (uniquement avant sa date de début)")
    public ResponseEntity<APIResponse<Void>> supprimerFermeture(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        horaireSalonService.supprimerFermeture(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Fermeture exceptionnelle supprimée avec succès", null));
    }

    @PutMapping("/fermetures/{id}")
    @Operation(summary = "Modifier une fermeture exceptionnelle selon les contraintes de dates")
    public ResponseEntity<APIResponse<FermetureExceptionnelleDTOResponse>> modifierFermeture(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody FermetureExceptionnelleDTORequest request,
            Principal principal) {

        FermetureExceptionnelleDTOResponse response = horaireSalonService.modifierFermeture(
                slugSalon, id, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Fermeture exceptionnelle modifiée avec succès", response));
    }

    @PatchMapping("/fermetures/{id}/mettre-fin")
    @Operation(summary = "Mettre fin immédiatement à une fermeture exceptionnelle en cours (date de fin devient aujourd'hui)")
    public ResponseEntity<APIResponse<FermetureExceptionnelleDTOResponse>> mettreFinFermeture(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        FermetureExceptionnelleDTOResponse response = horaireSalonService.mettreFinFermeture(
                slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Fermeture exceptionnelle clôturée avec succès (date de fin ramenée à aujourd'hui)", response));
    }

    // --- GESTION DES SERVICES ET VARIANTES ---

    @PostMapping(value = "/services", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Créer un service avec ses variantes et image (Multipart/form-data)")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> creerService(
            @PathVariable String slugSalon,
            @Valid @org.springframework.web.bind.annotation.RequestPart("data") ServiceSalonCreateDTORequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "image", required = false) MultipartFile image,
            Principal principal) throws IOException {

        ServiceSalonDTOResponse response = serviceSalonService.creerService(slugSalon, request, image, principal.getName(),
                "PROPRIETAIRE");
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
    @Operation(summary = "Modifier les attributs (nom, description) d'un service (JSON)")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> modifierService(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody ServiceSalonUpdateDTORequest request,
            Principal principal) {

        ServiceSalonDTOResponse response = serviceSalonService.modifierService(slugSalon, id, request,
                principal.getName(), "PROPRIETAIRE");
        return ResponseEntity.ok(new APIResponse<>(true, "Service mis à jour avec succès", response));
    }

    @PatchMapping(value = "/services/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour l'image d'un service (supprime l'ancienne sur Cloudinary)")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> uploadImageService(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {

        ServiceSalonDTOResponse response = serviceSalonService.uploadImageService(slugSalon, id, file,
                principal.getName(), "PROPRIETAIRE");
        return ResponseEntity.ok(new APIResponse<>(true, "Image du service mise à jour avec succès", response));
    }

    @PatchMapping("/services/{id}/statut")
    @Operation(summary = "Activer ou désactiver un service")
    public ResponseEntity<APIResponse<ServiceSalonDTOResponse>> basculerStatutService(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        ServiceSalonDTOResponse response = serviceSalonService.basculerStatutService(slugSalon, id, principal.getName(),
                "PROPRIETAIRE");
        return ResponseEntity.ok(new APIResponse<>(true, "Statut du service mis à jour", response));
    }

    @PostMapping(value = "/services/{id}/variantes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ajouter une variante à un service avec image (Multipart/form-data)")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> ajouterVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestPart("data") VarianteCreateDTORequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "image", required = false) MultipartFile image,
            Principal principal) throws IOException {

        VarianteServiceDTOResponse response = serviceSalonService.ajouterVariante(slugSalon, id, request, image,
                principal.getName(), "PROPRIETAIRE");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Variante ajoutée avec succès", response));
    }

    @PutMapping(value = "/services/{id}/variantes/{varId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier les attributs d'une variante de service (nom, durée, prix) (JSON)")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> modifierVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @PathVariable Long varId,
            @Valid @RequestBody VarianteUpdateDTORequest request,
            Principal principal) {

        VarianteServiceDTOResponse response = serviceSalonService.modifierVariante(slugSalon, id, varId, request,
                principal.getName(), "PROPRIETAIRE");
        return ResponseEntity.ok(new APIResponse<>(true, "Variante mise à jour avec succès", response));
    }

    @PatchMapping(value = "/services/{id}/variantes/{varId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mettre à jour l'image d'une variante (supprime l'ancienne sur Cloudinary)")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> uploadImageVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @PathVariable Long varId,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {

        VarianteServiceDTOResponse response = serviceSalonService.uploadImageVariante(slugSalon, id, varId, file,
                principal.getName(), "PROPRIETAIRE");
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

        VarianteServiceDTOResponse response = serviceSalonService.modifierPrixVariante(slugSalon, id, varId, request,
                principal.getName(), "PROPRIETAIRE");
        return ResponseEntity.ok(new APIResponse<>(true, "Prix de la variante mis à jour", response));
    }

    @PatchMapping("/services/{id}/variantes/{varId}/statut")
    @Operation(summary = "Activer ou désactiver une variante de service")
    public ResponseEntity<APIResponse<VarianteServiceDTOResponse>> basculerStatutVariante(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @PathVariable Long varId,
            Principal principal) {

        VarianteServiceDTOResponse response = serviceSalonService.basculerStatutVariante(slugSalon, id, varId,
                principal.getName(), "PROPRIETAIRE");
        return ResponseEntity.ok(new APIResponse<>(true, "Statut de la variante mis à jour", response));
    }

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

        CompteDTOResponse compte = compteService.updateProfil(principal.getName(), request, slugSalon, "PROPRIETAIRE");
        return ResponseEntity.ok(new APIResponse<>(true, "Profil mis à jour avec succès", compte));
    }

    @PatchMapping("/compte/mot-de-passe")
    @Operation(summary = "Modifier son mot de passe en fournissant l'ancien et le nouveau")
    public ResponseEntity<APIResponse<Void>> changerMotDePasse(
            @PathVariable String slugSalon,
            @Valid @RequestBody ChangementMotDePasseDTORequest request,
            Principal principal) {

        compteService.changerMotDePasse(principal.getName(), request, slugSalon, "PROPRIETAIRE");
        return ResponseEntity.ok(new APIResponse<>(true, "Mot de passe modifié avec succès", null));
    }

    @GetMapping("/mes-roles")
    @Operation(summary = "Consulter ses rôles actuels dans le salon")
    public ResponseEntity<APIResponse<EmployeDTOResponse>> getMesRoles(
            @PathVariable String slugSalon,
            Principal principal) {

        EmployeDTOResponse response = proprietaireSalonService.getMesRoles(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Rôles du propriétaire récupérés avec succès", response));
    }

    @PutMapping("/mes-roles")
    @Operation(summary = "Mettre à jour ses propres rôles dans le salon (le rôle PROPRIETAIRE reste obligatoirement conservé)")
    public ResponseEntity<APIResponse<EmployeDTOResponse>> updateMesRoles(
            @PathVariable String slugSalon,
            @Valid @RequestBody EmployeUpdateRolesDTORequest request,
            Principal principal) {

        EmployeDTOResponse response = proprietaireSalonService.updateMesRoles(slugSalon, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Vos rôles ont été mis à jour avec succès", response));
    }

    @PostMapping("/transfert")
    @Operation(summary = "Transférer la propriété du salon à un autre compte (seule opération permettant de céder le rôle PROPRIETAIRE)")
    public ResponseEntity<APIResponse<SalonDTOResponse>> transfererPropriete(
            @PathVariable String slugSalon,
            @Valid @RequestBody TransfertProprieteDTORequest request,
            Principal principal) {

        SalonDTOResponse response = proprietaireSalonService.transfererPropriete(slugSalon, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "La propriété du salon a été transférée avec succès", response));
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

    @PatchMapping("/indisponibilites/{id}/mettre-fin")
    @Operation(summary = "Mettre fin immédiatement à une indisponibilité en cours (date de fin devient maintenant)")
    public ResponseEntity<APIResponse<IndisponibiliteDTOResponse>> mettreFinIndisponibilite(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        IndisponibiliteDTOResponse response = coiffeurSalonService.mettreFinIndisponibiliteParManagerOuProprio(
                slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Indisponibilité clôturée avec succès (date de fin ramenée à maintenant)", response));
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

    // --- PLANNING DU SALON EN LECTURE SEULE ---

    @GetMapping("/planning")
    @Operation(summary = "Consulter le planning du salon en lecture seule (avec filtre optionnel par date et coiffeur)")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse>>> getPlanning(
            @PathVariable String slugSalon,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate date,
            @RequestParam(required = false) Long coiffeurAffectationId) {

        List<com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse> planning =
                receptionnisteSalonService.consulterPlanning(slugSalon, date, coiffeurAffectationId);
        return ResponseEntity.ok(new APIResponse<>(true, "Planning du salon récupéré avec succès", planning));
    }
}

