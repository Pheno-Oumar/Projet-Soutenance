package com.kadi_aon.mon_salon.rendezvous.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.coiffeur.entity.IndisponibiliteCoiffeur;
import com.kadi_aon.mon_salon.coiffeur.repository.IndisponibiliteCoiffeurRepository;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousCreateDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.entity.LigneRendezVous;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.mapper.RendezVousDTOResponseMapper;
import com.kadi_aon.mon_salon.rendezvous.repository.LigneRendezVousRepository;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.HoraireOuverture;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.entity.VarianteService;
import com.kadi_aon.mon_salon.salon.enums.JourSemaine;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.FermetureExceptionnelleRepository;
import com.kadi_aon.mon_salon.salon.repository.HoraireOuvertureRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.salon.repository.VarianteServiceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RendezVousService {

    private final SalonRepository salonRepository;
    private final CompteRepository compteRepository;
    private final VarianteServiceRepository varianteServiceRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final RoleSalonRepository roleSalonRepository;
    private final FermetureExceptionnelleRepository fermetureExceptionnelleRepository;
    private final HoraireOuvertureRepository horaireOuvertureRepository;
    private final IndisponibiliteCoiffeurRepository indisponibiliteCoiffeurRepository;
    private final LigneRendezVousRepository ligneRendezVousRepository;
    private final RendezVousRepository rendezVousRepository;
    private final PrestationRepository prestationRepository;
    private final DisponibiliteService disponibiliteService;
    private final RendezVousDTOResponseMapper rendezVousDTOResponseMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public RendezVousDTOResponse creerRendezVousClient(
            String slugSalon,
            RendezVousCreateDTORequest request,
            String clientEmail) {

        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        if (!Boolean.TRUE.equals(salon.getStatut())) {
            throw new IllegalArgumentException("Ce salon est actuellement fermé ou inactif.");
        }

        Compte client = compteRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Compte client introuvable : " + clientEmail));

        // Vérifier ou rattacher le rôle CLIENT dans ce salon
        AffectationSalon clientAffectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(clientEmail, slugSalon)
                .orElseGet(() -> {
                    RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)
                            .orElseThrow(() -> new IllegalStateException("Rôle CLIENT non initialisé"));
                    Set<RoleSalon> roles = new HashSet<>();
                    roles.add(roleClient);

                    AffectationSalon nouvelle = AffectationSalon.builder()
                            .compte(client)
                            .salon(salon)
                            .roles(roles)
                            .statut(true)
                            .dateDebut(LocalDate.now())
                            .build();
                    return affectationSalonRepository.save(nouvelle);
                });

        LocalDateTime debutPrevue = request.dateHeurePrevue();
        LocalDate date = debutPrevue.toLocalDate();
        LocalTime heureDebut = debutPrevue.toLocalTime();

        // 1. Validation stricte des variantes : statut variante ET statut du service parent
        List<VarianteService> variantes = new ArrayList<>();
        int dureeTotale = 0;
        BigDecimal montantTotal = BigDecimal.ZERO;

        for (Long varId : request.varianteIds()) {
            VarianteService variante = varianteServiceRepository.findById(varId)
                    .orElseThrow(() -> new EntityNotFoundException("Prestation introuvable avec l'ID : " + varId));

            if (!variante.getServiceSalon().getSalon().getId().equals(salon.getId())) {
                throw new IllegalArgumentException("La prestation '" + variante.getNom() + "' n'appartient pas à ce salon.");
            }

            if (!Boolean.TRUE.equals(variante.getStatut()) || !Boolean.TRUE.equals(variante.getServiceSalon().getStatut())) {
                throw new IllegalArgumentException("La prestation '" + variante.getNom() + "' ou son service associé est inactif.");
            }

            variantes.add(variante);
            dureeTotale += variante.getDureeMinutes();
            montantTotal = montantTotal.add(variante.getPrix());
        }

        LocalDateTime finPrevue = debutPrevue.plusMinutes(dureeTotale);
        LocalTime heureFin = finPrevue.toLocalTime();

        // 2. Vérifier les fermetures exceptionnelles
        if (!fermetureExceptionnelleRepository.findChevauchements(salon.getId(), date, date).isEmpty()) {
            throw new IllegalArgumentException("Le salon est exceptionnellement fermé à cette date.");
        }

        // 3. Vérifier les horaires réguliers
        JourSemaine jourSemaine = JourSemaine.from(date.getDayOfWeek());
        HoraireOuverture horaire = horaireOuvertureRepository.findBySalonIdAndJourSemaine(salon.getId(), jourSemaine)
                .orElseThrow(() -> new IllegalArgumentException("Le salon est fermé le " + jourSemaine));

        if (heureDebut.isBefore(horaire.getHeureOuverture()) || heureFin.isAfter(horaire.getHeureFermeture())) {
            throw new IllegalArgumentException("Le rendez-vous dépasse les horaires d'ouverture du salon ("
                    + horaire.getHeureOuverture() + " - " + horaire.getHeureFermeture() + ").");
        }

        if (horaire.getPauseDebut() != null && horaire.getPauseFin() != null) {
            if (heureDebut.isBefore(horaire.getPauseFin()) && heureFin.isAfter(horaire.getPauseDebut())) {
                throw new IllegalArgumentException("Le créneau sélectionné chevauche la pause méridienne du salon.");
            }
        }

        // 4. Vérifier que le client n'a pas déjà un autre RDV actif sur cette période dans ce salon
        List<RendezVous> rdvExistantsClient = rendezVousRepository.findRendezVousActifsByClientAndSalonAndPeriode(
                client.getId(),
                salon.getId(),
                debutPrevue,
                finPrevue,
                StatutRendezVous.ANNULE
        );
        if (!rdvExistantsClient.isEmpty()) {
            throw new IllegalArgumentException("Vous possédez déjà un rendez-vous sur cette plage horaire dans ce salon.");
        }

        // 5. Coiffeurs éligibles
        List<AffectationSalon> coiffeursEligibles = affectationSalonRepository.findBySalonIdAndStatutTrue(salon.getId()).stream()
                .filter(a -> a.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR))
                .toList();

        if (request.coiffeurId() != null) {
            coiffeursEligibles = coiffeursEligibles.stream()
                    .filter(c -> c.getId().equals(request.coiffeurId()))
                    .toList();

            if (coiffeursEligibles.isEmpty()) {
                throw new IllegalArgumentException("Le coiffeur sélectionné n'est pas actif dans ce salon.");
            }
        }

        if (coiffeursEligibles.isEmpty()) {
            throw new IllegalArgumentException("Aucun coiffeur actif dans ce salon.");
        }

        // Récupération des indispos et lignes RDV du jour
        LocalDateTime debutJour = date.atStartOfDay();
        LocalDateTime finJour = date.atTime(23, 59, 59);

        List<IndisponibiliteCoiffeur> indispos = indisponibiliteCoiffeurRepository.findBySalonIdAndPeriode(salon.getId(), debutJour, finJour);
        List<LigneRendezVous> lignesRDV = ligneRendezVousRepository.findLignesActivesBySalonAndPeriode(salon.getId(), debutJour, finJour, StatutRendezVous.ANNULE);

        List<LigneRendezVous> lignesCreer = new ArrayList<>();

        // Mode mono-coiffeur exclusif (Scénario A validé)
        AffectationSalon coiffeurLibre = null;
        for (AffectationSalon c : coiffeursEligibles) {
            if (disponibiliteService.isCoiffeurLibre(c.getId(), debutPrevue, finPrevue, indispos, lignesRDV)) {
                coiffeurLibre = c;
                break;
            }
        }

        if (coiffeurLibre == null) {
            throw new IllegalArgumentException("Aucun coiffeur disponible sur l'ensemble de la durée du rendez-vous.");
        }

        LocalDateTime subStart = debutPrevue;
        int ordre = 1;
        for (VarianteService v : variantes) {
            LocalDateTime subEnd = subStart.plusMinutes(v.getDureeMinutes());
            LigneRendezVous ligne = LigneRendezVous.builder()
                    .varianteService(v)
                    .dateHeureDebut(subStart)
                    .dateHeureFin(subEnd)
                    .duree(v.getDureeMinutes())
                    .prix(v.getPrix())
                    .ordre(ordre++)
                    .build();
            lignesCreer.add(ligne);
            subStart = subEnd;
        }

        // Construction et enregistrement du RendezVous
        RendezVous rdv = RendezVous.builder()
                .salon(salon)
                .coiffeur(coiffeurLibre)
                .client(client)
                .dateHeurePrevue(debutPrevue)
                .dateHeureFin(finPrevue)
                .statut(StatutRendezVous.CONFIRME)
                .montantEstime(montantTotal)
                .build();

        for (LigneRendezVous l : lignesCreer) {
            rdv.addLigne(l);
        }

        RendezVous saved = rendezVousRepository.save(rdv);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "RendezVous",
                String.valueOf(saved.getId()),
                null,
                String.format("RDV le %s, montant=%s, nbPrestations=%d", saved.getDateHeurePrevue(), saved.getMontantEstime(), saved.getLignes().size()),
                clientAffectation,
                "CLIENT"
        );

        log.info("Rendez-vous {} créé avec succès pour le client {} dans le salon {}", saved.getId(), clientEmail, slugSalon);
        return rendezVousDTOResponseMapper.apply(saved);
    }

    @Transactional(readOnly = true)
    public List<RendezVousDTOResponse> listerMesRendezVous(String slugSalon, String clientEmail) {
        return rendezVousRepository.findBySalonSlugAndClientEmailOrderByDateHeurePrevueDesc(slugSalon, clientEmail)
                .stream()
                .map(rendezVousDTOResponseMapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public RendezVousDTOResponse getDetailRendezVous(String slugSalon, Long rdvId, String clientEmail) {
        RendezVous rdv = rendezVousRepository.findByIdAndSalonSlug(rdvId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Rendez-vous introuvable avec l'identifiant : " + rdvId));

        if (rdv.getClient() == null || !rdv.getClient().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à consulter ce rendez-vous.");
        }

        return rendezVousDTOResponseMapper.apply(rdv);
    }

    @Transactional(readOnly = true)
    public List<RendezVousDTOResponse> listerTousMesRendezVous(String clientEmail) {
        return rendezVousRepository.findByClientEmailOrderByDateHeurePrevueDesc(clientEmail)
                .stream()
                .map(rendezVousDTOResponseMapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public RendezVousDTOResponse getDetailRendezVousGlobal(Long rdvId, String clientEmail) {
        RendezVous rdv = rendezVousRepository.findById(rdvId)
                .orElseThrow(() -> new EntityNotFoundException("Rendez-vous introuvable ID " + rdvId));

        if (rdv.getClient() == null || !rdv.getClient().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à consulter ce rendez-vous.");
        }

        return rendezVousDTOResponseMapper.apply(rdv);
    }

    @Transactional

    public RendezVousDTOResponse annulerRendezVousClient(
            String slugSalon,
            Long rdvId,
            com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest request,
            String clientEmail) {

        RendezVous rdv = rendezVousRepository.findByIdAndSalonSlug(rdvId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Rendez-vous introuvable avec l'identifiant : " + rdvId));

        if (rdv.getClient() == null || !rdv.getClient().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à annuler ce rendez-vous.");
        }

        if (!rdv.isTransitionAutorisee(StatutRendezVous.ANNULE)) {
            throw new IllegalStateException("Transition de statut non autorisée vers ANNULE pour ce rendez-vous (statut actuel : " + rdv.getStatut() + ").");
        }

        // Validation croisée : interdire si la prestation liée est EN_COURS ou TERMINEE, annuler si EN_ATTENTE
        prestationRepository.findByRendezVousId(rdv.getId()).ifPresent(prestation -> {
            if (prestation.getStatut() == StatutPrestation.EN_COURS || prestation.getStatut() == StatutPrestation.TERMINEE) {
                throw new IllegalStateException(
                        "Impossible d'annuler ce rendez-vous : la prestation est " + prestation.getStatut().name().toLowerCase() + ".");
            }
            if (prestation.getStatut() == StatutPrestation.EN_ATTENTE) {
                prestation.setStatut(StatutPrestation.ANNULEE);
                prestation.setDateHeureFin(LocalDateTime.now());
                prestationRepository.save(prestation);
            }
        });

        if (request.motif() == null || request.motif().trim().isEmpty()) {
            throw new IllegalArgumentException("Le motif d'annulation est obligatoire.");
        }

        // L'annulation client doit s'effectuer avant le début prévu du rendez-vous
        if (rdv.getDateHeurePrevue() != null && LocalDateTime.now().isAfter(rdv.getDateHeurePrevue())) {
            throw new IllegalStateException("Le délai d'annulation est dépassé : l'heure prévue du rendez-vous est déjà passée.");
        }

        rdv.setStatut(StatutRendezVous.ANNULE);
        rdv.setMotifAnnulation(request.motif().trim());
        rdv.setDateAnnulation(LocalDateTime.now());

        RendezVous saved = rendezVousRepository.save(rdv);

        AffectationSalon clientAffectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(clientEmail, slugSalon)
                .orElse(null);

        if (clientAffectation != null) {
            auditLogService.logActionSalon(
                    TypeActionAudit.MODIFICATION,
                    "RendezVous",
                    String.valueOf(saved.getId()),
                    "CONFIRME",
                    "ANNULE : " + request.motif(),
                    clientAffectation,
                    "CLIENT"
            );
        }

        log.info("Rendez-vous {} annulé avec succès par le client {}", rdvId, clientEmail);
        return rendezVousDTOResponseMapper.apply(saved);
    }
}
