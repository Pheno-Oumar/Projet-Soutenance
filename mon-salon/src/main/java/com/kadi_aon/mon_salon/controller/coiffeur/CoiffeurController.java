package com.kadi_aon.mon_salon.controller.coiffeur;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.coiffeur.dto.CoiffeurDashboardDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.service.CoiffeurSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.profilcapillaire.dto.CodeProfilVerificationDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.service.ProfilCapillaireService;
import com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/{slugSalon}/coiffeur")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'COIFFEUR')")
@RequiredArgsConstructor
@Tag(name = "Espace Coiffeur", description = "Gestion du profil coiffeur, indisponibilités, compte, clients et profil capillaire")
public class CoiffeurController {

    private final CoiffeurSalonService coiffeurSalonService;
    private final CompteService compteService;
    private final ProfilCapillaireService profilCapillaireService;
    private final com.kadi_aon.mon_salon.avis.service.AvisSalonService avisSalonService;

    // --- DASHBOARD COIFFEUR ---

    @GetMapping("/dashboard")
    @Operation(summary = "Obtenir le tableau de bord du coiffeur (statistiques du jour, prestations du mois, planning du jour, avis)")
    public ResponseEntity<APIResponse<CoiffeurDashboardDTOResponse>> getDashboard(
            @PathVariable String slugSalon,
            Principal principal) {
        CoiffeurDashboardDTOResponse response = coiffeurSalonService.obtenirDashboardCoiffeur(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Dashboard coiffeur récupéré avec succès", response));
    }

    // --- CLIENTS DU SALON ---

    @GetMapping("/clients")
    @Operation(summary = "Lister les clients du salon pour sélection et consultation du profil capillaire")
    public ResponseEntity<APIResponse<List<ClientSalonResumeDTOResponse>>> getClients(
            @PathVariable String slugSalon,
            @RequestParam(required = false) String search,
            Principal principal) {
        List<ClientSalonResumeDTOResponse> response = coiffeurSalonService.listerClientsPourCoiffeur(slugSalon, search, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Liste des clients récupérée avec succès", response));
    }

    @PostMapping("/profil-capillaire/{clientId}/consulter")
    @Operation(summary = "Consulter le profil capillaire d'un client en saisissant son code d'accès PIN (6 chiffres)")
    public ResponseEntity<APIResponse<ProfilCapillaireDTOResponse>> consulterProfilCapillaireClient(
            @PathVariable String slugSalon,
            @PathVariable Long clientId,
            @Valid @RequestBody CodeProfilVerificationDTORequest request,
            Principal principal) {

        ProfilCapillaireDTOResponse response = profilCapillaireService.consulterProfilClientParCoiffeur(
                slugSalon, clientId, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Profil capillaire déverrouillé avec succès", response));
    }

    @GetMapping("/compte")
    @Operation(summary = "Consulter ses informations de compte")
    public ResponseEntity<APIResponse<CompteDTOResponse>> getCompte(Principal principal) {
        CompteDTOResponse response = compteService.getProfil(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Informations du compte récupérées", response));
    }

    @PutMapping("/compte")
    @Operation(summary = "Modifier ses informations personnelles de compte")
    public ResponseEntity<APIResponse<CompteDTOResponse>> updateCompte(
            @PathVariable String slugSalon,
            @Valid @RequestBody CompteUpdateDTORequest request,
            Principal principal) {
        CompteDTOResponse response = compteService.updateProfil(principal.getName(), request, slugSalon, "COIFFEUR");
        return ResponseEntity.ok(new APIResponse<>(true, "Compte mis à jour avec succès", response));
    }

    @PatchMapping("/compte/mot-de-passe")
    @Operation(summary = "Modifier son mot de passe en fournissant l'ancien et le nouveau")
    public ResponseEntity<APIResponse<Void>> changerMotDePasse(
            @PathVariable String slugSalon,
            @Valid @RequestBody ChangementMotDePasseDTORequest request,
            Principal principal) {
        compteService.changerMotDePasse(principal.getName(), request, slugSalon, "COIFFEUR");
        return ResponseEntity.ok(new APIResponse<>(true, "Mot de passe modifié avec succès", null));
    }

    @GetMapping("/profil")
    @Operation(summary = "Consulter son profil coiffeur")
    public ResponseEntity<APIResponse<ProfilCoiffeurDTOResponse>> getProfil(
            @PathVariable String slugSalon,
            Principal principal) {

        ProfilCoiffeurDTOResponse response = coiffeurSalonService.getProfil(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Profil coiffeur récupéré", response));
    }

    @PutMapping("/profil")
    @Operation(summary = "Mettre à jour son profil coiffeur")
    public ResponseEntity<APIResponse<ProfilCoiffeurDTOResponse>> updateProfil(
            @PathVariable String slugSalon,
            @Valid @RequestBody ProfilCoiffeurDTORequest request,
            Principal principal) {

        ProfilCoiffeurDTOResponse response = coiffeurSalonService.updateProfil(slugSalon, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Profil coiffeur mis à jour", response));
    }

    @org.springframework.web.bind.annotation.PatchMapping(path = "/profil/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader la photo de profil du coiffeur sur Cloudinary (format WebP)")
    public ResponseEntity<APIResponse<ProfilCoiffeurDTOResponse>> uploadPhotoProfil(
            @PathVariable String slugSalon,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Principal principal) throws java.io.IOException {

        ProfilCoiffeurDTOResponse response = coiffeurSalonService.uploadPhotoProfil(slugSalon, file,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Photo de profil mise à jour avec succès", response));
    }

    @GetMapping("/indisponibilites")
    @Operation(summary = "Lister ses indisponibilités déclarées")
    public ResponseEntity<APIResponse<List<IndisponibiliteDTOResponse>>> listerIndisponibilites(
            @PathVariable String slugSalon,
            Principal principal) {

        List<IndisponibiliteDTOResponse> response = coiffeurSalonService.listerMesIndisponibilites(slugSalon,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Indisponibilités récupérées", response));
    }

    @PostMapping("/indisponibilites")
    @Operation(summary = "Déclarer une nouvelle indisponibilité personnelle (congé, absence)")
    public ResponseEntity<APIResponse<IndisponibiliteDTOResponse>> ajouterIndisponibilite(
            @PathVariable String slugSalon,
            @Valid @RequestBody IndisponibiliteDTORequest request,
            Principal principal) {

        IndisponibiliteDTOResponse response = coiffeurSalonService.ajouterIndisponibilite(slugSalon, request,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Indisponibilité déclarée avec succès", response));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/indisponibilites/{id}")
    @Operation(summary = "Supprimer une de ses indisponibilités avant sa date de début")
    public ResponseEntity<APIResponse<Void>> supprimerIndisponibilite(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {

        coiffeurSalonService.supprimerIndisponibilite(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Indisponibilité supprimée avec succès", null));
    }

    // --- PLANNING DU COIFFEUR ---

    @GetMapping("/planning")
    @Operation(summary = "Consulter son planning de rendez-vous dans le salon (par jour/date)")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse>>> getPlanning(
            @PathVariable String slugSalon,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            Principal principal) {

        List<com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse> planning = coiffeurSalonService
                .obtenirPlanningCoiffeur(slugSalon, principal.getName(), date);
        return ResponseEntity.ok(new APIResponse<>(true, "Planning coiffeur récupéré avec succès", planning));
    }

    // --- AVIS DU COIFFEUR ---

    @GetMapping("/avis")
    @Operation(summary = "Consulter les avis concernant le coiffeur sur ce salon")
    public ResponseEntity<APIResponse<List<com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse>>> getAvis(
            @PathVariable String slugSalon,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean statut,
            Principal principal) {

        List<com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse> avis = avisSalonService
                .listerAvisPourCoiffeur(slugSalon, principal.getName(), statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Avis coiffeur récupérés avec succès", avis));
    }
}
