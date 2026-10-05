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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.service.CompteService;
import com.kadi_aon.mon_salon.avis.dto.AvisPrestationDTOResponse;
import com.kadi_aon.mon_salon.avis.dto.AvisSalonDTOResponse;
import com.kadi_aon.mon_salon.avis.service.AvisSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.favori.dto.FavoriCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.favori.dto.FavoriSalonDTOResponse;
import com.kadi_aon.mon_salon.favori.service.FavoriSalonService;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.profilcapillaire.dto.CodeProfilDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.service.ProfilCapillaireService;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationCreateDTORequest;
import com.kadi_aon.mon_salon.reclamation.dto.ReclamationDTOResponse;
import com.kadi_aon.mon_salon.reclamation.service.ReclamationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.service.RendezVousService;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeExportDTOResponse;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTORequest;
import com.kadi_aon.mon_salon.rgpd.dto.DemandeSuppressionDTOResponse;
import com.kadi_aon.mon_salon.rgpd.service.RgpdClientService;
import com.kadi_aon.mon_salon.stock.dto.CommandeDTOResponse;
import com.kadi_aon.mon_salon.stock.dto.PanierDTOResponse;
import com.kadi_aon.mon_salon.stock.service.CommandeSalonService;
import com.kadi_aon.mon_salon.stock.service.PanierService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/client")
@PreAuthorize("@salonSecurity.isClient()")
@RequiredArgsConstructor
@Tag(name = "Client Plateforme (Transversal)", description = "Espace personnel du client sur la plateforme : vue transversale multi-salons de ses rendez-vous, paiements, paniers, commandes, favoris, profil capillaire et RGPD")
public class ClientPlateformeController {

    private final CompteService compteService;
    private final ProfilCapillaireService profilCapillaireService;
    private final RendezVousService rendezVousService;
    private final PrestationSalonService prestationSalonService;
    private final FacturationSalonService facturationSalonService;
    private final FavoriSalonService favoriSalonService;
    private final AvisSalonService avisSalonService;
    private final PanierService panierService;
    private final CommandeSalonService commandeSalonService;
    private final ReclamationSalonService reclamationSalonService;
    private final RgpdClientService rgpdClientService;

    // ==========================================
    // 1. COMPTE & PROFIL PERSONNEL GLOBAL
    // ==========================================

    @GetMapping("/compte")
    @Operation(summary = "Consulter ses informations personnelles de compte")
    public ResponseEntity<APIResponse<CompteDTOResponse>> getCompte(Principal principal) {
        CompteDTOResponse response = compteService.getProfil(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Informations du compte récupérées", response));
    }

    @PutMapping("/compte")
    @Operation(summary = "Modifier ses informations personnelles de compte")
    public ResponseEntity<APIResponse<CompteDTOResponse>> updateCompte(
            @Valid @RequestBody CompteUpdateDTORequest request,
            Principal principal) {
        CompteDTOResponse response = compteService.updateProfil(principal.getName(), request, null, "CLIENT");
        return ResponseEntity.ok(new APIResponse<>(true, "Compte mis à jour avec succès", response));
    }

    @PatchMapping("/compte/mot-de-passe")
    @PutMapping("/compte/mot-de-passe")
    @Operation(summary = "Modifier son mot de passe")
    public ResponseEntity<APIResponse<Void>> changerMotDePasse(
            @Valid @RequestBody ChangementMotDePasseDTORequest request,
            Principal principal) {
        compteService.changerMotDePasse(principal.getName(), request, null, "CLIENT");
        return ResponseEntity.ok(new APIResponse<>(true, "Mot de passe modifié avec succès", null));
    }

    // ==========================================
    // 2. PROFIL CAPILLAIRE (GLOBAL AU COMPTE)
    // ==========================================

    @GetMapping("/profil-capillaire")
    @Operation(summary = "Consulter son profil capillaire global")
    public ResponseEntity<APIResponse<ProfilCapillaireDTOResponse>> getMonProfilCapillaire(Principal principal) {
        ProfilCapillaireDTOResponse response = profilCapillaireService.getMonProfil(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Profil capillaire récupéré avec succès", response));
    }

    @PutMapping("/profil-capillaire")
    @Operation(summary = "Créer ou mettre à jour son profil capillaire")
    public ResponseEntity<APIResponse<ProfilCapillaireDTOResponse>> updateMonProfilCapillaire(
            @Valid @RequestBody ProfilCapillaireDTORequest request,
            Principal principal) {
        ProfilCapillaireDTOResponse response = profilCapillaireService.enregistrerOuModifierProfil(principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Profil capillaire mis à jour avec succès", response));
    }

    @PatchMapping("/profil-capillaire/code")
    @Operation(summary = "Définir ou modifier le code secret d'accès (PIN) au profil capillaire")
    public ResponseEntity<APIResponse<Void>> changerCodeProfil(
            @Valid @RequestBody CodeProfilDTORequest request,
            Principal principal) {
        profilCapillaireService.definirOuChangerCodePin(principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Code d'accès PIN mis à jour avec succès", null));
    }

    // ==========================================
    // 3. RENDEZ-VOUS (TRANSVERSAUX OU PAR SALON)
    // ==========================================

    @GetMapping("/rendez-vous")
    @Operation(summary = "Consulter tous ses rendez-vous (tous salons ou filtré par salon)")
    public ResponseEntity<APIResponse<List<RendezVousDTOResponse>>> listerMesRendezVous(
            @RequestParam(required = false) String slugSalon,
            Principal principal) {

        List<RendezVousDTOResponse> response = (slugSalon != null && !slugSalon.isBlank())
                ? rendezVousService.listerMesRendezVous(slugSalon, principal.getName())
                : rendezVousService.listerTousMesRendezVous(principal.getName());

        return ResponseEntity.ok(new APIResponse<>(true, "Liste de vos rendez-vous récupérée", response));
    }

    @GetMapping("/rendez-vous/{id}")
    @Operation(summary = "Consulter le détail d'un de ses rendez-vous")
    public ResponseEntity<APIResponse<RendezVousDTOResponse>> getDetailRendezVous(
            @PathVariable Long id,
            Principal principal) {

        RendezVousDTOResponse response = rendezVousService.getDetailRendezVousGlobal(id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Détail du rendez-vous récupéré", response));
    }

    // ==========================================
    // 4. PRESTATIONS (HISTORIQUE GLOBAL)
    // ==========================================

    @GetMapping("/prestations")
    @Operation(summary = "Consulter son historique de prestations (tous salons ou filtré par salon)")
    public ResponseEntity<APIResponse<List<PrestationDTOResponse>>> listerMesPrestations(
            @RequestParam(required = false) String slugSalon,
            Principal principal) {

        List<PrestationDTOResponse> response = (slugSalon != null && !slugSalon.isBlank())
                ? prestationSalonService.listerPrestationsClientSalon(slugSalon, principal.getName())
                : prestationSalonService.listerToutesPrestationsClient(principal.getName());

        return ResponseEntity.ok(new APIResponse<>(true, "Historique de vos prestations récupéré", response));
    }

    @GetMapping("/prestations/{id}")
    @Operation(summary = "Consulter le détail d'une prestation passée")
    public ResponseEntity<APIResponse<PrestationDTOResponse>> getDetailPrestation(
            @PathVariable Long id,
            Principal principal) {

        PrestationDTOResponse response = prestationSalonService.obtenirDetailPrestationClient(id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la prestation récupéré", response));
    }

    // ==========================================
    // 5. PAIEMENTS (TRANSVERSAUX)
    // ==========================================

    @GetMapping("/paiements")
    @Operation(summary = "Consulter ses paiements (tous salons confondus ou filtré par salon)")
    public ResponseEntity<APIResponse<List<PaiementDTOResponse>>> listerMesPaiements(
            @RequestParam(required = false) String slugSalon,
            Principal principal) {

        List<PaiementDTOResponse> response = (slugSalon != null && !slugSalon.isBlank())
                ? facturationSalonService.listerPaiementsClient(slugSalon, principal.getName())
                : facturationSalonService.listerTousPaiementsClient(principal.getName());

        return ResponseEntity.ok(new APIResponse<>(true, "Liste de vos paiements récupérée", response));
    }

    @GetMapping("/paiements/{id}")
    @Operation(summary = "Consulter le détail d'un de ses paiements")
    public ResponseEntity<APIResponse<PaiementDTOResponse>> getDetailPaiement(
            @PathVariable Long id,
            Principal principal) {

        PaiementDTOResponse response = facturationSalonService.obtenirDetailPaiementClientGlobal(id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Détail du paiement récupéré", response));
    }

    // ==========================================
    // 6. SALONS FAVORIS
    // ==========================================

    @GetMapping({"/favoris/salons", "/favoris"})
    @Operation(summary = "Consulter la liste de ses salons favoris")
    public ResponseEntity<APIResponse<List<FavoriSalonDTOResponse>>> listerMesSalonsFavoris(Principal principal) {
        List<FavoriSalonDTOResponse> response = favoriSalonService.listerMesSalonsFavoris(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Liste de vos salons favoris", response));
    }

    @GetMapping("/favoris/coiffeurs")
    @Operation(summary = "Consulter la liste de ses coiffeurs favoris (tous salons confondus)")
    public ResponseEntity<APIResponse<List<FavoriCoiffeurDTOResponse>>> listerMesCoiffeursFavoris(Principal principal) {
        List<FavoriCoiffeurDTOResponse> response = favoriSalonService.listerTousMesCoiffeursFavoris(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Liste de vos coiffeurs favoris récupérée", response));
    }

    @PostMapping("/favoris/{slugSalon}")
    @Operation(summary = "Ajouter un salon à ses favoris")
    public ResponseEntity<APIResponse<FavoriSalonDTOResponse>> ajouterSalonFavori(
            @PathVariable String slugSalon,
            Principal principal) {

        FavoriSalonDTOResponse response = favoriSalonService.ajouterSalonFavori(slugSalon, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Salon ajouté à vos favoris", response));
    }

    @DeleteMapping("/favoris/{slugSalon}")
    @Operation(summary = "Retirer un salon de ses favoris")
    public ResponseEntity<APIResponse<Void>> retirerSalonFavori(
            @PathVariable String slugSalon,
            Principal principal) {

        favoriSalonService.retirerSalonFavori(slugSalon, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Salon retiré de vos favoris", null));
    }

    // ==========================================
    // 7. AVIS (SALONS ET PRESTATIONS)
    // ==========================================

    @GetMapping("/avis/salons")
    @Operation(summary = "Consulter la liste de tous ses avis déposés sur les salons")
    public ResponseEntity<APIResponse<List<AvisSalonDTOResponse>>> listerMesAvisSalons(Principal principal) {
        List<AvisSalonDTOResponse> response = avisSalonService.listerTousMesAvisSalons(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Vos avis salons récupérés", response));
    }

    @GetMapping("/avis/prestations")
    @Operation(summary = "Consulter la liste de tous ses avis déposés sur les prestations")
    public ResponseEntity<APIResponse<List<AvisPrestationDTOResponse>>> listerMesAvisPrestations(Principal principal) {
        List<AvisPrestationDTOResponse> response = avisSalonService.listerTousMesAvisPrestations(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Vos avis prestations récupérés", response));
    }

    // ==========================================
    // 8. PANIERS (PAR SALON)
    // ==========================================

    @GetMapping("/paniers")
    @Operation(summary = "Consulter tous ses paniers d'achat actifs groupés par salon")
    public ResponseEntity<APIResponse<List<PanierDTOResponse>>> listerMesPaniers(Principal principal) {
        List<PanierDTOResponse> response = panierService.listerTousMesPaniers(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Vos paniers actifs groupés par salon", response));
    }

    @DeleteMapping("/paniers")
    @Operation(summary = "Vider tous ses paniers actifs dans tous les salons")
    public ResponseEntity<APIResponse<Void>> viderTousMesPaniers(Principal principal) {
        panierService.viderTousMesPaniers(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Tous vos paniers ont été vidés", null));
    }

    // ==========================================
    // 9. COMMANDES (HISTORIQUE GROUPÉ)
    // ==========================================

    @GetMapping("/commandes")
    @Operation(summary = "Consulter l'historique complet de ses commandes de produits (tous salons confondus)")
    public ResponseEntity<APIResponse<List<CommandeDTOResponse>>> listerMesCommandes(Principal principal) {
        List<CommandeDTOResponse> response = commandeSalonService.listerToutesCommandesClient(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Historique de vos commandes récupéré", response));
    }

    @GetMapping("/commandes/{id}")
    @Operation(summary = "Consulter le détail d'une commande")
    public ResponseEntity<APIResponse<CommandeDTOResponse>> getDetailCommande(
            @PathVariable Long id,
            Principal principal) {

        CommandeDTOResponse response = commandeSalonService.obtenirCommandeClientGlobal(principal.getName(), id);
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la commande récupéré", response));
    }

    // ==========================================
    // 10. RÉCLAMATIONS
    // ==========================================

    @GetMapping("/reclamations")
    @Operation(summary = "Consulter toutes ses réclamations déposées (tous salons confondus)")
    public ResponseEntity<APIResponse<List<ReclamationDTOResponse>>> listerMesReclamations(Principal principal) {
        List<ReclamationDTOResponse> response = reclamationSalonService.listerToutesReclamationsClient(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Vos réclamations récupérées", response));
    }

    @GetMapping("/reclamations/{id}")
    @Operation(summary = "Consulter le détail d'une réclamation")
    public ResponseEntity<APIResponse<ReclamationDTOResponse>> getDetailReclamation(
            @PathVariable Long id,
            Principal principal) {

        ReclamationDTOResponse response = reclamationSalonService.obtenirReclamationClientGlobal(id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Détail de la réclamation récupéré", response));
    }

    @PostMapping("/reclamations")
    @Operation(summary = "Déposer une nouvelle réclamation en ciblant un salon spécifique")
    public ResponseEntity<APIResponse<ReclamationDTOResponse>> deposerReclamation(
            @RequestParam String slugSalon,
            @Valid @RequestBody ReclamationCreateDTORequest request,
            Principal principal) {

        ReclamationDTOResponse response = reclamationSalonService.deposerReclamation(slugSalon, principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Réclamation déposée avec succès", response));
    }

    // ==========================================
    // 11. RGPD (EXPORT ET SUPPRESSION)
    // ==========================================

    @PostMapping("/rgpd/export")
    @Operation(summary = "Demander l'export de ses données personnelles (Droit à la portabilité)")
    public ResponseEntity<APIResponse<DemandeExportDTOResponse>> demanderExport(
            @Valid @RequestBody(required = false) DemandeExportDTORequest request,
            Principal principal) {

        DemandeExportDTOResponse response = rgpdClientService.demanderExportDonnees(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Demande d'export générée avec succès", response));
    }

    @GetMapping("/rgpd/archives")
    @Operation(summary = "Consulter ses archives d'exportation disponibles")
    public ResponseEntity<APIResponse<List<DemandeExportDTOResponse>>> consulterArchives(Principal principal) {
        List<DemandeExportDTOResponse> response = rgpdClientService.consulterMesExports(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Archives d'exportation récupérées", response));
    }


    @PostMapping("/rgpd/suppression")
    @Operation(summary = "Demander la suppression de son compte et de ses données (Droit à l'oubli)")
    public ResponseEntity<APIResponse<DemandeSuppressionDTOResponse>> demanderSuppression(
            @Valid @RequestBody(required = false) DemandeSuppressionDTORequest request,
            Principal principal) {

        DemandeSuppressionDTOResponse response = rgpdClientService.demanderSuppressionCompte(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Demande de suppression enregistrée", response));
    }

    @GetMapping("/rgpd/suivi")
    @Operation(summary = "Consulter le suivi de ses demandes de suppression de compte")
    public ResponseEntity<APIResponse<List<DemandeSuppressionDTOResponse>>> consulterSuiviSuppression(Principal principal) {
        List<DemandeSuppressionDTOResponse> response = rgpdClientService.consulterMesDemandesSuppression(principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Suivi des demandes de suppression", response));
    }
}
