package com.kadi_aon.mon_salon.controller.receptionniste;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseClotureDTORequest;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseDTOResponse;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseOuvertureDTORequest;
import com.kadi_aon.mon_salon.caisse.service.CaisseSalonService;
import com.kadi_aon.mon_salon.common.dto.response.APIResponse;
import com.kadi_aon.mon_salon.facturation.dto.FactureDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTORequest;
import com.kadi_aon.mon_salon.facturation.dto.PaiementDTOResponse;
import com.kadi_aon.mon_salon.facturation.dto.RemboursementDTORequest;
import com.kadi_aon.mon_salon.facturation.dto.RemiseDTORequest;
import com.kadi_aon.mon_salon.facturation.service.FacturationSalonService;
import com.kadi_aon.mon_salon.prestation.dto.PrestationCreateDTORequest;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.CreneauDisponibleDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.DisponibiliteSearchDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardTraitementDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardNotificationDTORequest;
import com.kadi_aon.mon_salon.rendezvous.service.DisponibiliteService;
import com.kadi_aon.mon_salon.salon.dto.ClientRapideDTORequest;
import com.kadi_aon.mon_salon.salon.dto.ClientRapideDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ReceptionnisteDashboardDTOResponse;
import com.kadi_aon.mon_salon.facturation.enums.StatutFacture;
import com.kadi_aon.mon_salon.rendezvous.dto.ReceptionnisteRDVCreateDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousCreateDTORequest;
import com.kadi_aon.mon_salon.rendezvous.service.RendezVousService;
import com.kadi_aon.mon_salon.salon.service.ReceptionnisteSalonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{slugSalon}/receptionniste")
@PreAuthorize("@salonSecurity.hasRoleInSalon(#slugSalon, 'RECEPTIONNISTE')")
@RequiredArgsConstructor
@Tag(name = "Espace Réceptionniste", description = "Tableau de bord, planning du salon, gestion des clients, prise de rendez-vous, prestations, caisse et encaissement")
public class ReceptionnisteController {

    private final ReceptionnisteSalonService receptionnisteSalonService;
    private final CompteService compteService;
    private final DisponibiliteService disponibiliteService;
    private final PrestationSalonService prestationSalonService;
    private final FacturationSalonService facturationSalonService;
    private final CaisseSalonService caisseSalonService;
    private final RendezVousService rendezVousService;

    // --- TABLEAU DE BORD OPÉRATIONNEL ---

    @GetMapping("/dashboard")
    @Operation(summary = "Consulter le tableau de bord opérationnel du réceptionniste (KPIs du jour, état caisse, prochains rendez-vous, retards et prestations en cours)")
    public ResponseEntity<APIResponse<ReceptionnisteDashboardDTOResponse>> getDashboard(
            @PathVariable String slugSalon) {
        ReceptionnisteDashboardDTOResponse response = receptionnisteSalonService.getDashboard(slugSalon);
        return ResponseEntity
                .ok(new APIResponse<>(true, "Tableau de bord réceptionniste récupéré avec succès", response));
    }

    // --- COMPTE PERSONNEL ---

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
        CompteDTOResponse response = compteService.updateProfil(principal.getName(), request, slugSalon,
                "RECEPTIONNISTE");
        return ResponseEntity.ok(new APIResponse<>(true, "Compte mis à jour avec succès", response));
    }

    @PutMapping("/compte/mot-de-passe")
    @PatchMapping("/compte/mot-de-passe")
    @Operation(summary = "Modifier son mot de passe en fournissant l'ancien et le nouveau")
    public ResponseEntity<APIResponse<Void>> changerMotDePasse(
            @PathVariable String slugSalon,
            @Valid @RequestBody ChangementMotDePasseDTORequest request,
            Principal principal) {
        compteService.changerMotDePasse(principal.getName(), request, slugSalon, "RECEPTIONNISTE");
        return ResponseEntity.ok(new APIResponse<>(true, "Mot de passe modifié avec succès", null));
    }

    // --- PLANNING & DISPONIBILITÉS ---

    @GetMapping("/planning")
    @Operation(summary = "Consulter le planning de la journée (avec filtre optionnel par coiffeur)")
    public ResponseEntity<APIResponse<List<PlanningRendezVousDTOResponse>>> consulterPlanning(
            @PathVariable String slugSalon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long coiffeurAffectationId) {

        List<PlanningRendezVousDTOResponse> response = receptionnisteSalonService.consulterPlanning(
                slugSalon, date, coiffeurAffectationId);
        return ResponseEntity.ok(new APIResponse<>(true, "Planning récupéré avec succès", response));
    }

    @PostMapping("/disponibilites")
    @Operation(summary = "Consulter les disponibilités des coiffeurs pour proposer un créneau")
    public ResponseEntity<APIResponse<List<CreneauDisponibleDTOResponse>>> consulterDisponibilites(
            @PathVariable String slugSalon,
            @Valid @RequestBody DisponibiliteSearchDTORequest request) {

        List<CreneauDisponibleDTOResponse> response = disponibiliteService.calculerDisponibilites(slugSalon, request);
        return ResponseEntity.ok(new APIResponse<>(true, "Créneaux disponibles récupérés", response));
    }

    @PostMapping("/rendez-vous")
    @Operation(summary = "Créer un rendez-vous pour un client au comptoir ou par téléphone")
    public ResponseEntity<APIResponse<RendezVousDTOResponse>> creerRendezVous(
            @PathVariable String slugSalon,
            @Valid @RequestBody ReceptionnisteRDVCreateDTORequest request) {
        RendezVousCreateDTORequest dtoReq = new RendezVousCreateDTORequest(
                request.dateHeurePrevue(),
                request.varianteIds(),
                request.coiffeurId()
        );
        RendezVousDTOResponse response = rendezVousService.creerRendezVousClient(slugSalon, dtoReq, request.clientEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Rendez-vous créé avec succès", response));
    }

    // --- GESTION DES CLIENTS ---

    @GetMapping("/clients")
    @Operation(summary = "Rechercher des clients par nom, prénom, email ou téléphone")
    public ResponseEntity<APIResponse<List<ClientRapideDTOResponse>>> rechercherClients(
            @PathVariable String slugSalon,
            @RequestParam(required = false, defaultValue = "") String query) {

        List<ClientRapideDTOResponse> response = receptionnisteSalonService.rechercherClients(slugSalon, query);
        return ResponseEntity.ok(new APIResponse<>(true, "Clients trouvés", response));
    }

    @PostMapping("/clients")
    @Operation(summary = "Créer rapidement un client dans le salon")
    public ResponseEntity<APIResponse<ClientRapideDTOResponse>> creerClientRapide(
            @PathVariable String slugSalon,
            @Valid @RequestBody ClientRapideDTORequest request,
            Principal principal) {

        ClientRapideDTOResponse response = receptionnisteSalonService.creerClientRapide(
                slugSalon, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Client enregistré avec succès", response));
    }

    // --- GESTION DES RETARDS & NO-SHOW ---

    @GetMapping("/retards")
    @Operation(summary = "Consulter les rendez-vous en retard pour la journée en cours")
    public ResponseEntity<APIResponse<List<RetardRendezVousDTOResponse>>> listerRendezVousEnRetard(
            @PathVariable String slugSalon) {

        List<RetardRendezVousDTOResponse> response = receptionnisteSalonService.listerRendezVousEnRetard(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Rendez-vous en retard récupérés", response));
    }

    @PostMapping("/retards/{id}/traiter")
    @Operation(summary = "Traiter un retard (décaler de X minutes ou déclarer NO_SHOW pour libérer le créneau)")
    public ResponseEntity<APIResponse<RendezVousDTOResponse>> traiterRetard(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody RetardTraitementDTORequest request,
            Principal principal) {

        RendezVousDTOResponse response = receptionnisteSalonService.traiterRetard(
                slugSalon, id, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Traitement du retard appliqué avec succès", response));
    }

    @GetMapping("/retards/historique")
    @Operation(summary = "Consulter l'historique des retards et no-shows du salon")
    public ResponseEntity<APIResponse<List<RetardRendezVousDTOResponse>>> listerHistoriqueRetards(
            @PathVariable String slugSalon) {

        List<RetardRendezVousDTOResponse> response = receptionnisteSalonService.listerHistoriqueRetards(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Historique des retards récupéré", response));
    }

    @PatchMapping("/rendez-vous/{id}/annuler")
    @Operation(summary = "Annuler un rendez-vous (à la demande du client ou du salon avec motif)")
    public ResponseEntity<APIResponse<RendezVousDTOResponse>> annulerRendezVous(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody RendezVousAnnulationDTORequest request,
            Principal principal) {
        RendezVousDTOResponse response = receptionnisteSalonService.annulerRendezVous(slugSalon, id, request,
                principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Rendez-vous annulé avec succès", response));
    }

    // --- PRESTATIONS & ENCAISSEMENT ---

    @PostMapping("/prestations")
    @Operation(summary = "Créer une prestation (depuis un rendez-vous ou directe/walk-in), générant automatiquement sa facture")
    public ResponseEntity<APIResponse<PrestationDTOResponse>> creerPrestation(
            @PathVariable String slugSalon,
            @Valid @RequestBody PrestationCreateDTORequest request,
            Principal principal) {
        PrestationDTOResponse response = prestationSalonService.creerPrestation(slugSalon, principal.getName(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Prestation créée et facture générée avec succès", response));
    }

    @GetMapping("/prestations/{id}")
    @Operation(summary = "Consulter le détail d'une prestation, ses lignes et sa facture")
    public ResponseEntity<APIResponse<PrestationDTOResponse>> getPrestation(
            @PathVariable String slugSalon,
            @PathVariable Long id) {
        PrestationDTOResponse response = prestationSalonService.obtenirPrestation(slugSalon, id);
        return ResponseEntity.ok(new APIResponse<>(true, "Prestation récupérée", response));
    }

    @GetMapping("/prestations")
    @Operation(summary = "Lister les prestations du salon (avec filtre statut optionnel)")
    public ResponseEntity<APIResponse<List<PrestationDTOResponse>>> listerPrestations(
            @PathVariable String slugSalon,
            @RequestParam(required = false) StatutPrestation statut) {
        List<PrestationDTOResponse> response = prestationSalonService.listerPrestations(slugSalon, statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Liste des prestations récupérée", response));
    }

    @PutMapping("/prestations/{id}/terminer")
    @Operation(summary = "Marquer une prestation comme terminée (clôture le rendez-vous lié et finalise la facture)")
    public ResponseEntity<APIResponse<PrestationDTOResponse>> terminerPrestation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        PrestationDTOResponse response = prestationSalonService.terminerPrestation(slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Prestation marquée comme terminée", response));
    }

    @PutMapping("/prestations/{id}/demarrer")
    @Operation(summary = "Démarrer une prestation en attente (passage au fauteuil de coiffure)")
    public ResponseEntity<APIResponse<PrestationDTOResponse>> demarrerPrestation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestParam(required = false) Long coiffeurAffectationId,
            Principal principal) {
        PrestationDTOResponse response = prestationSalonService.demarrerPrestation(slugSalon, id, coiffeurAffectationId, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Prestation démarrée avec succès", response));
    }

    @PutMapping("/rendez-vous/{id}/permuter")
    @Operation(summary = "Permuter le coiffeur d'un rendez-vous vers un autre coiffeur")
    public ResponseEntity<APIResponse<PlanningRendezVousDTOResponse>> permuterCoiffeurRendezVous(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestParam Long nouveauCoiffeurAffectationId,
            Principal principal) {
        PlanningRendezVousDTOResponse response = receptionnisteSalonService.permuterCoiffeurRendezVous(
                slugSalon, id, nouveauCoiffeurAffectationId, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Coiffeur du rendez-vous permuté avec succès", response));
    }

    @PostMapping("/rendez-vous/{id}/notifier-retard")
    @Operation(summary = "Notifier le client d'un léger retard avec un message personnalisé")
    public ResponseEntity<APIResponse<String>> notifierRetardClient(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @Valid @RequestBody RetardNotificationDTORequest request,
            Principal principal) {
        receptionnisteSalonService.notifierRetardClient(slugSalon, id, request, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Notification de retard envoyée avec succès au client", request.message()));
    }

    @PostMapping("/rendez-vous/{id}/pointer-arrivee")
    @Operation(summary = "Pointer l'arrivée d'un rendez-vous et l'installer dans la file d'attente (Lounge)")
    public ResponseEntity<APIResponse<PrestationDTOResponse>> pointerArriveeRendezVous(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            Principal principal) {
        PrestationDTOResponse response = receptionnisteSalonService.pointerArriveeRendezVous(
                slugSalon, id, principal.getName());
        return ResponseEntity.ok(new APIResponse<>(true, "Arrivée du client confirmée, prestation ajoutée en salle d'attente", response));
    }

    @PutMapping("/prestations/{id}/annuler")
    @Operation(summary = "Annuler une prestation")
    public ResponseEntity<APIResponse<PrestationDTOResponse>> annulerPrestation(
            @PathVariable String slugSalon,
            @PathVariable Long id,
            @RequestParam(required = false) String motif,
            Principal principal) {
        PrestationDTOResponse response = prestationSalonService.annulerPrestation(slugSalon, id, principal.getName(), motif);
        return ResponseEntity.ok(new APIResponse<>(true, "Prestation annulée avec succès", response));
    }

    @PostMapping("/factures/{factureId}/paiements")
    @Operation(summary = "Encaisser un paiement lié à une facture et générer une écriture d'entrée en caisse")
    public ResponseEntity<APIResponse<PaiementDTOResponse>> encaisserPaiement(
            @PathVariable String slugSalon,
            @PathVariable Long factureId,
            @Valid @RequestBody PaiementDTORequest request,
            Principal principal) {
        PaiementDTOResponse response = facturationSalonService.encaisserPaiement(slugSalon, factureId,
                principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Paiement encaissé avec succès", response));
    }

    @PostMapping("/paiements/{paiementId}/remboursement")
    @Operation(summary = "Effectuer le remboursement d'un paiement existant et générer une écriture de sortie de caisse")
    public ResponseEntity<APIResponse<PaiementDTOResponse>> effectuerRemboursement(
            @PathVariable String slugSalon,
            @PathVariable Long paiementId,
            @Valid @RequestBody RemboursementDTORequest request,
            Principal principal) {
        PaiementDTOResponse response = facturationSalonService.effectuerRemboursement(slugSalon, paiementId,
                principal.getName(), request);
        return ResponseEntity.ok(new APIResponse<>(true, "Remboursement effectué avec succès", response));
    }

    @GetMapping("/factures")
    @Operation(summary = "Lister les factures du salon avec filtre statut optionnel (EMISE, PARTIELLEMENT_PAYEE, PAYEE, IMPAYEE)")
    public ResponseEntity<APIResponse<List<FactureDTOResponse>>> listerFactures(
            @PathVariable String slugSalon,
            @RequestParam(required = false) String statut) {
        List<FactureDTOResponse> response = facturationSalonService.listerFacturesSalon(slugSalon, statut);
        return ResponseEntity.ok(new APIResponse<>(true, "Factures du salon récupérées", response));
    }

    @GetMapping("/factures/recherche")
    @Operation(summary = "Rechercher une facture par son numéro officiel (ex: FAC-BEAUTY-IN-BLACK-...)")
    public ResponseEntity<APIResponse<FactureDTOResponse>> rechercherFactureParNumero(
            @PathVariable String slugSalon,
            @RequestParam String numero) {
        FactureDTOResponse response = facturationSalonService.rechercherFactureParNumero(slugSalon, numero);
        return ResponseEntity.ok(new APIResponse<>(true, "Facture trouvée", response));
    }

    @GetMapping("/factures/{factureId}")
    @Operation(summary = "Consulter le détail d'une facture et l'état de ses paiements")
    public ResponseEntity<APIResponse<FactureDTOResponse>> getFacture(
            @PathVariable String slugSalon,
            @PathVariable Long factureId) {
        FactureDTOResponse response = facturationSalonService.obtenirFactureDetail(slugSalon, factureId);
        return ResponseEntity.ok(new APIResponse<>(true, "Facture récupérée", response));
    }

    @PatchMapping("/factures/{factureId}/remise")
    @Operation(summary = "Appliquer une remise commerciale sur une facture")
    public ResponseEntity<APIResponse<FactureDTOResponse>> appliquerRemise(
            @PathVariable String slugSalon,
            @PathVariable Long factureId,
            @Valid @RequestBody RemiseDTORequest request,
            Principal principal) {
        FactureDTOResponse response = facturationSalonService.appliquerRemise(
                slugSalon, factureId, principal.getName(), request.montant(), request.motif());
        return ResponseEntity.ok(new APIResponse<>(true, "Remise appliquée avec succès", response));
    }

    // --- CAISSE DU SALON ---

    @PostMapping("/caisse/sessions")
    @Operation(summary = "Ouvrir une nouvelle session de caisse par le réceptionniste")
    public ResponseEntity<APIResponse<SessionCaisseDTOResponse>> ouvrirSessionCaisse(
            @PathVariable String slugSalon,
            @Valid @RequestBody SessionCaisseOuvertureDTORequest request,
            Principal principal) {
        SessionCaisseDTOResponse response = caisseSalonService.ouvrirSessionCaisse(slugSalon, principal.getName(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse<>(true, "Session de caisse ouverte avec succès", response));
    }

    @PutMapping("/caisse/sessions/courante/cloturer")
    @Operation(summary = "Clôturer la session de caisse en cours par le réceptionniste")
    public ResponseEntity<APIResponse<SessionCaisseDTOResponse>> cloturerSessionCaisse(
            @PathVariable String slugSalon,
            @Valid @RequestBody SessionCaisseClotureDTORequest request,
            Principal principal) {
        SessionCaisseDTOResponse response = caisseSalonService.cloturerSessionCaisse(slugSalon, principal.getName(),
                request);
        return ResponseEntity.ok(new APIResponse<>(true, "Session de caisse clôturée avec succès", response));
    }

    @GetMapping("/caisse/sessions/courante")
    @Operation(summary = "Consulter la session de caisse active du salon")
    public ResponseEntity<APIResponse<SessionCaisseDTOResponse>> getSessionCourante(
            @PathVariable String slugSalon) {
        SessionCaisseDTOResponse response = caisseSalonService.obtenirSessionCourante(slugSalon);
        return ResponseEntity.ok(new APIResponse<>(true, "Session courante récupérée", response));
    }
}
