package com.kadi_aon.mon_salon.salon.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.TypeOperationCaisse;
import com.kadi_aon.mon_salon.caisse.repository.OperationCaisseRepository;
import com.kadi_aon.mon_salon.caisse.repository.SessionCaisseRepository;
import com.kadi_aon.mon_salon.notification.service.NotificationEmailService;
import com.kadi_aon.mon_salon.prestation.dto.LignePrestationDTORequest;
import com.kadi_aon.mon_salon.prestation.dto.PrestationCreateDTORequest;
import com.kadi_aon.mon_salon.prestation.dto.PrestationDTOResponse;
import com.kadi_aon.mon_salon.prestation.entity.Prestation;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.prestation.service.PrestationSalonService;
import com.kadi_aon.mon_salon.rendezvous.dto.LigneRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousAnnulationDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardRendezVousDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardTraitementDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.RetardNotificationDTORequest;
import com.kadi_aon.mon_salon.rendezvous.entity.LigneRendezVous;
import com.kadi_aon.mon_salon.rendezvous.entity.RendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.ActionTraitementRetard;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.mapper.RendezVousDTOResponseMapper;
import com.kadi_aon.mon_salon.rendezvous.repository.RendezVousRepository;
import com.kadi_aon.mon_salon.salon.dto.ClientRapideDTORequest;
import com.kadi_aon.mon_salon.salon.dto.ClientRapideDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ReceptionnisteCaisseStatutDTO;
import com.kadi_aon.mon_salon.salon.dto.ReceptionnisteDashboardDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.ReceptionnisteStatsJourDTO;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.RoleSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceptionnisteSalonService {

        private final SalonRepository salonRepository;
        private final CompteRepository compteRepository;
        private final AffectationSalonRepository affectationSalonRepository;
        private final RoleSalonRepository roleSalonRepository;
        private final RendezVousRepository rendezVousRepository;
        private final RendezVousDTOResponseMapper rendezVousDTOResponseMapper;
        private final PasswordEncoder passwordEncoder;
        private final AuditLogService auditLogService;
        private final PrestationRepository prestationRepository;
        private final PrestationSalonService prestationSalonService;
        private final SessionCaisseRepository sessionCaisseRepository;
        private final OperationCaisseRepository operationCaisseRepository;
        private final NotificationEmailService notificationEmailService;

        // --- TABLEAU DE BORD RÉCEPTIONNISTE ---

        @Transactional(readOnly = true)
        public ReceptionnisteDashboardDTOResponse getDashboard(String slugSalon) {
                Salon salon = verifierSalonActif(slugSalon);
                LocalDate today = LocalDate.now();
                LocalDateTime debutJour = today.atStartOfDay();
                LocalDateTime finJour = today.atTime(23, 59, 59);
                LocalDateTime now = LocalDateTime.now();

                // 1. Planning du jour
                List<RendezVous> rdvJour = rendezVousRepository.findPlanningBySalonAndJour(salon.getId(), debutJour, finJour);

                long totalRdvJour = rdvJour.size();
                long rdvEnAttente = rdvJour.stream()
                                .filter(r -> r.getStatut() == StatutRendezVous.CONFIRME && r.getDateHeurePrevue().isAfter(now))
                                .count();
                long noShowsCount = rdvJour.stream()
                                .filter(r -> r.getStatut() == StatutRendezVous.NO_SHOW)
                                .count();

                // 2. Prestations du jour
                List<PrestationDTOResponse> prestationsEnCours = prestationSalonService.listerPrestations(slugSalon, StatutPrestation.EN_COURS);
                long prestationsEnCoursCount = prestationsEnCours.size();

                List<Prestation> allPrestationsSalon = prestationRepository.findBySalonSlugOrderByDateHeureDebutDesc(slugSalon);
                long prestationsTerminees = allPrestationsSalon.stream()
                                .filter(p -> p.getStatut() == StatutPrestation.TERMINEE
                                                && p.getDateHeureDebut() != null
                                                && !p.getDateHeureDebut().isBefore(debutJour)
                                                && !p.getDateHeureDebut().isAfter(finJour))
                                .count();

                // 3. Retards actuels
                List<RetardRendezVousDTOResponse> retardsActuels = listerRendezVousEnRetard(slugSalon);
                long clientsEnRetardCount = retardsActuels.size();

                ReceptionnisteStatsJourDTO stats = new ReceptionnisteStatsJourDTO(
                                totalRdvJour,
                                rdvEnAttente,
                                prestationsEnCoursCount,
                                prestationsTerminees,
                                clientsEnRetardCount,
                                noShowsCount
                );

                // 4. Statut Caisse
                ReceptionnisteCaisseStatutDTO caisseStatut;
                Optional<SessionCaisse> sessionCaisseOpt = sessionCaisseRepository
                                .findByAffectationSalonSlugAndStatut(slugSalon, StatutSessionCaisse.EN_COURS);

                if (sessionCaisseOpt.isPresent()) {
                        SessionCaisse session = sessionCaisseOpt.get();
                        List<OperationCaisse> operations = operationCaisseRepository
                                        .findBySessionCaisseIdOrderByDateOperationDesc(session.getId());

                        BigDecimal totalEncaisse = operations.stream()
                                        .filter(op -> op.getType() == TypeOperationCaisse.ENTREE)
                                        .map(OperationCaisse::getMontant)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        caisseStatut = new ReceptionnisteCaisseStatutDTO(
                                        true,
                                        session.getId(),
                                        session.getDateOuverture(),
                                        session.getSoldeOuverture(),
                                        totalEncaisse,
                                        operations.size()
                        );
                } else {
                        caisseStatut = new ReceptionnisteCaisseStatutDTO(
                                        false,
                                        null,
                                        null,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        0
                        );
                }

                // 5. Prochains rendez-vous (5 max)
                List<PlanningRendezVousDTOResponse> prochainsRendezVous = rdvJour.stream()
                                .filter(r -> r.getStatut() == StatutRendezVous.CONFIRME && r.getDateHeureFin().isAfter(now))
                                .sorted((r1, r2) -> r1.getDateHeurePrevue().compareTo(r2.getDateHeurePrevue()))
                                .limit(5)
                                .map(this::mapToPlanningResponse)
                                .toList();

                return new ReceptionnisteDashboardDTOResponse(
                                stats,
                                caisseStatut,
                                prochainsRendezVous,
                                retardsActuels,
                                prestationsEnCours
                );
        }

        // --- RECHERCHE ET CRÉATION RAPIDE DE CLIENT ---

        @Transactional(readOnly = true)
        public List<ClientRapideDTOResponse> rechercherClients(String slugSalon, String query) {
                verifierSalonActif(slugSalon);
                if (query == null || query.isBlank()) {
                        return List.of();
                }

                return compteRepository.searchClients(query.trim()).stream()
                                .map(c -> new ClientRapideDTOResponse(
                                                c.getId(),
                                                c.getNom(),
                                                c.getPrenom(),
                                                c.getTelephone(),
                                                c.getEmail(),
                                                c.getDateNaissance()))
                                .toList();
        }

        @Transactional
        public ClientRapideDTOResponse creerClientRapide(String slugSalon, ClientRapideDTORequest request,
                        String receptionnisteEmail) {
                Salon salon = verifierSalonActif(slugSalon);
                AffectationSalon affectationRecep = getAffectationReceptionniste(receptionnisteEmail, slugSalon);

                String email = request.email();
                if (email == null || email.isBlank()) {
                        throw new IllegalArgumentException("L'adresse email est obligatoire pour créer un compte client.");
                }

                Compte compte = compteRepository.findByEmail(email).orElse(null);
                if (compte == null) {
                        compte = Compte.builder()
                                        .nom(request.nom())
                                        .prenom(request.prenom())
                                        .telephone(request.telephone())
                                        .email(email)
                                        .dateNaissance(request.dateNaissance())
                                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                        .statut(true)
                                        .build();
                        compte = compteRepository.save(compte);
                }

                RoleSalon roleClient = roleSalonRepository.findByRole(TypeRoleSalon.CLIENT)
                                .orElseThrow(() -> new EntityNotFoundException("Rôle salon CLIENT introuvable."));

                final Compte finalCompte = compte;
                AffectationSalon affectationClient = affectationSalonRepository
                                .findByCompteEmailAndSalonSlugAndStatutTrue(finalCompte.getEmail(), slugSalon)
                                .orElseGet(() -> {
                                        AffectationSalon aff = AffectationSalon.builder()
                                                        .salon(salon)
                                                        .compte(finalCompte)
                                                        .statut(true)
                                                        .dateDebut(LocalDate.now())
                                                        .roles(new java.util.HashSet<>(List.of(roleClient)))
                                                        .build();
                                        return affectationSalonRepository.save(aff);
                                });

                auditLogService.logActionSalon(
                                TypeActionAudit.CREATION,
                                "Compte",
                                String.valueOf(compte.getId()),
                                null,
                                "Création rapide client : " + compte.getNom() + " " + compte.getPrenom(),
                                affectationRecep,
                                "RECEPTIONNISTE");

                log.info("Client rapide créé/rattaché ID {} par la réceptionniste {}", compte.getId(),
                                receptionnisteEmail);
                return new ClientRapideDTOResponse(
                                compte.getId(),
                                compte.getNom(),
                                compte.getPrenom(),
                                compte.getTelephone(),
                                compte.getEmail(),
                                compte.getDateNaissance());
        }

        // --- PLANNING DU SALON ---

        @Transactional(readOnly = true)
        public List<PlanningRendezVousDTOResponse> consulterPlanning(String slugSalon, LocalDate date,
                        Long coiffeurAffectationId) {
                Salon salon = verifierSalonActif(slugSalon);
                LocalDate dateConsultation = (date != null) ? date : LocalDate.now();
                LocalDateTime debutJour = dateConsultation.atStartOfDay();
                LocalDateTime finJour = dateConsultation.atTime(23, 59, 59);

                List<RendezVous> rdvList;
                if (coiffeurAffectationId != null) {
                        rdvList = rendezVousRepository.findPlanningBySalonAndCoiffeurAndJour(salon.getId(),
                                        coiffeurAffectationId, debutJour, finJour);
                } else {
                        rdvList = rendezVousRepository.findPlanningBySalonAndJour(salon.getId(), debutJour, finJour);
                }

                return rdvList.stream()
                                .map(this::mapToPlanningResponse)
                                .toList();
        }

        // --- GESTION DES RETARDS & NO-SHOW ---

        @Transactional(readOnly = true)
        public List<RetardRendezVousDTOResponse> listerRendezVousEnRetard(String slugSalon) {
                Salon salon = verifierSalonActif(slugSalon);
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime debutJour = LocalDate.now().atStartOfDay();

                List<RendezVous> enRetard = rendezVousRepository.findRendezVousEnRetard(
                                salon.getId(), now, debutJour, StatutRendezVous.CONFIRME);

                return enRetard.stream()
                                .map(r -> {
                                        long minutesRetard = ChronoUnit.MINUTES.between(r.getDateHeurePrevue(), now);
                                        String coiffeurNom = (r.getCoiffeur() != null
                                                        && r.getCoiffeur().getCompte() != null)
                                                                        ? r.getCoiffeur().getCompte().getNom() + " "
                                                                                        + r.getCoiffeur().getCompte()
                                                                                                        .getPrenom()
                                                                        : "Non assigné";

                                        return new RetardRendezVousDTOResponse(
                                                        r.getId(),
                                                        r.getClient() != null ? r.getClient().getNom() : null,
                                                        r.getClient() != null ? r.getClient().getPrenom() : null,
                                                        r.getClient() != null ? r.getClient().getTelephone() : null,
                                                        r.getDateHeurePrevue(),
                                                        r.getDateHeureFin(),
                                                        minutesRetard,
                                                        coiffeurNom,
                                                        r.getStatut().name());
                                })
                                .toList();
        }

        @Transactional
        public RendezVousDTOResponse traiterRetard(
                        String slugSalon,
                        Long rdvId,
                        RetardTraitementDTORequest request,
                        String receptionnisteEmail) {

                RendezVous rdv = getRendezVousSalon(slugSalon, rdvId);
                AffectationSalon affectationRecep = getAffectationReceptionniste(receptionnisteEmail, slugSalon);

                if (rdv.getStatut() != StatutRendezVous.CONFIRME) {
                        throw new IllegalArgumentException(
                                        "Seul un rendez-vous CONFIRME peut être traité pour retard.");
                }

                if (request.action() == ActionTraitementRetard.NO_SHOW) {
                        if (rdv.getDateHeurePrevue() != null
                                        && !LocalDateTime.now().isAfter(rdv.getDateHeurePrevue().plusMinutes(15))) {
                                throw new IllegalStateException(
                                                "Le statut NO_SHOW ne peut être appliqué que si le client a plus de 15 minutes de retard sur l'heure prévue.");
                        }

                        // Le créneau est libéré immédiatement pour la clientèle
                        rdv.setStatut(StatutRendezVous.NO_SHOW);
                        rdv.setMotifAnnulation("NO_SHOW : "
                                        + (request.motif() != null ? request.motif() : "Client non présenté"));
                        rdv.setDateAnnulation(LocalDateTime.now());

                        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "RendezVous",
                                        String.valueOf(rdv.getId()), "CONFIRME", "NO_SHOW - Créneau libéré",
                                        affectationRecep, "RECEPTIONNISTE");

                        log.info("Rendez-vous {} passé en NO_SHOW par {}", rdvId, receptionnisteEmail);
                } else if (request.action() == ActionTraitementRetard.DECALER) {
                        int decalage = (request.minutesDecalage() != null && request.minutesDecalage() > 0)
                                        ? request.minutesDecalage()
                                        : 15;

                        LocalDateTime nouveauDebut = rdv.getDateHeurePrevue().plusMinutes(decalage);
                        LocalDateTime nouvelleFin = rdv.getDateHeureFin().plusMinutes(decalage);

                        rdv.setDateHeurePrevue(nouveauDebut);
                        rdv.setDateHeureFin(nouvelleFin);

                        for (LigneRendezVous ligne : rdv.getLignes()) {
                                ligne.setDateHeureDebut(ligne.getDateHeureDebut().plusMinutes(decalage));
                                ligne.setDateHeureFin(ligne.getDateHeureFin().plusMinutes(decalage));
                        }

                        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "RendezVous",
                                        String.valueOf(rdv.getId()), "RETARD", "DECALER de " + decalage + " min",
                                        affectationRecep, "RECEPTIONNISTE");

                        log.info("Rendez-vous {} décalé de {} min par {}", rdvId, decalage, receptionnisteEmail);
                } else if (request.action() == ActionTraitementRetard.SIGNALER_RETARD) {
                        rdv.setMotifAnnulation("RETARD SIGNALE : "
                                        + (request.motif() != null ? request.motif() : "En attente du client"));

                        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "RendezVous",
                                        String.valueOf(rdv.getId()), "CONFIRME", "SIGNALER_RETARD", affectationRecep,
                                        "RECEPTIONNISTE");
                }

                RendezVous saved = rendezVousRepository.save(rdv);
                return rendezVousDTOResponseMapper.apply(saved);
        }

        @Transactional(readOnly = true)
        public List<RetardRendezVousDTOResponse> listerHistoriqueRetards(String slugSalon) {
                Salon salon = verifierSalonActif(slugSalon);
                LocalDateTime now = LocalDateTime.now();

                List<RendezVous> historique = rendezVousRepository.findHistoriqueRetards(salon.getId(),
                                StatutRendezVous.NO_SHOW);

                return historique.stream()
                                .map(r -> {
                                        long minutesRetard = ChronoUnit.MINUTES.between(r.getDateHeurePrevue(), now);
                                        String coiffeurNom = (r.getCoiffeur() != null
                                                        && r.getCoiffeur().getCompte() != null)
                                                                        ? r.getCoiffeur().getCompte().getNom() + " "
                                                                                        + r.getCoiffeur().getCompte()
                                                                                                        .getPrenom()
                                                                        : "Non assigné";

                                        return new RetardRendezVousDTOResponse(
                                                        r.getId(),
                                                        r.getClient() != null ? r.getClient().getNom() : null,
                                                        r.getClient() != null ? r.getClient().getPrenom() : null,
                                                        r.getClient() != null ? r.getClient().getTelephone() : null,
                                                        r.getDateHeurePrevue(),
                                                        r.getDateHeureFin(),
                                                        minutesRetard,
                                                        coiffeurNom,
                                                        r.getStatut().name());
                                })
                                .toList();
        }

        @Transactional
        public RendezVousDTOResponse annulerRendezVous(
                        String slugSalon,
                        Long rdvId,
                        RendezVousAnnulationDTORequest request,
                        String receptionnisteEmail) {

                RendezVous rdv = getRendezVousSalon(slugSalon, rdvId);
                AffectationSalon affectationRecep = getAffectationReceptionniste(receptionnisteEmail, slugSalon);

                if (rdv.getStatut() != StatutRendezVous.CONFIRME) {
                        throw new IllegalArgumentException(
                                        "Seul un rendez-vous avec le statut CONFIRME peut être annulé (statut actuel : " + rdv.getStatut() + ").");
                }

                // Validation croisée : interdire l'annulation si la prestation liée est EN_COURS
                prestationRepository.findByRendezVousId(rdv.getId()).ifPresent(prestation -> {
                        if (prestation.getStatut() == StatutPrestation.EN_COURS) {
                                throw new IllegalStateException(
                                        "Impossible d'annuler ce rendez-vous : la prestation liée (ID " + prestation.getId() + ") est actuellement EN_COURS. Terminez ou annulez la prestation d'abord.");
                        }
                });

                if (request.motif() == null || request.motif().trim().isEmpty()) {
                        throw new IllegalArgumentException("Le motif d'annulation est obligatoire.");
                }

                rdv.setStatut(StatutRendezVous.ANNULE);
                rdv.setMotifAnnulation(request.motif().trim());
                rdv.setDateAnnulation(LocalDateTime.now());

                auditLogService.logActionSalon(
                                TypeActionAudit.MODIFICATION,
                                "RendezVous",
                                String.valueOf(rdv.getId()),
                                "CONFIRME",
                                "ANNULE par réceptionniste : " + request.motif().trim(),
                                affectationRecep,
                                "RECEPTIONNISTE"
                );

                log.info("Rendez-vous {} annulé par la réceptionniste {} pour motif : {}", rdvId, receptionnisteEmail, request.motif());
                RendezVous saved = rendezVousRepository.save(rdv);
                return rendezVousDTOResponseMapper.apply(saved);
        }

        // --- SYNCHRONISATION RDV & COIFFEURS ---

        @Transactional
        public PlanningRendezVousDTOResponse permuterCoiffeurRendezVous(
                        String slugSalon,
                        Long rdvId,
                        Long nouveauCoiffeurAffectationId,
                        String receptionnisteEmail) {

                RendezVous rdv = getRendezVousSalon(slugSalon, rdvId);
                AffectationSalon affectationRecep = getAffectationReceptionniste(receptionnisteEmail, slugSalon);

                if (rdv.getStatut() != StatutRendezVous.CONFIRME && rdv.getStatut() != StatutRendezVous.EN_COURS) {
                        throw new IllegalStateException("Seul un rendez-vous CONFIRME ou EN_COURS peut voir son coiffeur permuter.");
                }

                AffectationSalon nouveauCoiffeur = affectationSalonRepository.findById(nouveauCoiffeurAffectationId)
                                .orElseThrow(() -> new EntityNotFoundException("Coiffeur introuvable ID " + nouveauCoiffeurAffectationId));

                if (!nouveauCoiffeur.getSalon().getId().equals(rdv.getSalon().getId())) {
                        throw new IllegalArgumentException("Le coiffeur sélectionné n'appartient pas au salon " + slugSalon);
                }

                boolean coiffeurActif = Boolean.TRUE.equals(nouveauCoiffeur.getStatut()) &&
                                nouveauCoiffeur.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR);
                if (!coiffeurActif) {
                        throw new IllegalArgumentException("L'employé sélectionné n'a pas le rôle COIFFEUR actif.");
                }

                String ancienCoiffeurNom = (rdv.getCoiffeur() != null && rdv.getCoiffeur().getCompte() != null)
                                ? rdv.getCoiffeur().getCompte().getPrenom() + " " + rdv.getCoiffeur().getCompte().getNom()
                                : "Non assigné";
                String nouveauCoiffeurNom = (nouveauCoiffeur.getCompte() != null)
                                ? nouveauCoiffeur.getCompte().getPrenom() + " " + nouveauCoiffeur.getCompte().getNom()
                                : "Inconnu";

                rdv.setCoiffeur(nouveauCoiffeur);
                rdv.setDateModification(LocalDateTime.now());

                // Si une prestation a déjà été créée pour ce rendez-vous (en attente ou en cours), synchroniser aussi son coiffeur
                prestationRepository.findByRendezVousId(rdv.getId()).ifPresent(prestation -> {
                        prestation.setCoiffeur(nouveauCoiffeur);
                        prestationRepository.save(prestation);
                });

                RendezVous saved = rendezVousRepository.save(rdv);

                auditLogService.logActionSalon(
                                TypeActionAudit.MODIFICATION,
                                "RendezVous",
                                String.valueOf(saved.getId()),
                                "COIFFEUR: " + ancienCoiffeurNom,
                                "COIFFEUR: " + nouveauCoiffeurNom,
                                affectationRecep,
                                "RECEPTIONNISTE");

                log.info("RDV {} permuté de {} vers {} par {}", rdvId, ancienCoiffeurNom, nouveauCoiffeurNom, receptionnisteEmail);
                return mapToPlanningResponse(saved);
        }

        @Transactional
        public void notifierRetardClient(
                        String slugSalon,
                        Long rdvId,
                        RetardNotificationDTORequest request,
                        String receptionnisteEmail) {

                RendezVous rdv = getRendezVousSalon(slugSalon, rdvId);
                AffectationSalon affectationRecep = getAffectationReceptionniste(receptionnisteEmail, slugSalon);

                String message = request.message();
                if (message == null || message.trim().isEmpty()) {
                        throw new IllegalArgumentException("Le message de notification ne peut pas être vide.");
                }

                String clientEmail = (rdv.getClient() != null) ? rdv.getClient().getEmail() : null;
                String clientTel = (rdv.getClient() != null) ? rdv.getClient().getTelephone() : null;
                String clientNom = (rdv.getClient() != null) ? rdv.getClient().getPrenom() + " " + rdv.getClient().getNom() : "Client";

                // Envoi email si client a un email renseigné
                if (clientEmail != null && !clientEmail.isBlank()) {
                        String sujet = "Information sur votre rendez-vous - " + rdv.getSalon().getNom();
                        notificationEmailService.queueNotificationEmail(clientEmail, sujet, message);
                }

                auditLogService.logActionSalon(
                                TypeActionAudit.MODIFICATION,
                                "RendezVous",
                                String.valueOf(rdv.getId()),
                                "NOTIFICATION_RETARD",
                                "Notification envoyée à " + clientNom + " (Tel: " + clientTel + ", Email: " + clientEmail + ") : " + message,
                                affectationRecep,
                                "RECEPTIONNISTE");

                log.info("Notification de retard envoyée pour RDV {} au client {} par {}", rdvId, clientNom, receptionnisteEmail);
        }

        @Transactional
        public PrestationDTOResponse pointerArriveeRendezVous(String slugSalon, Long rdvId, String receptionnisteEmail) {
                RendezVous rdv = getRendezVousSalon(slugSalon, rdvId);

                if (rdv.getStatut() != StatutRendezVous.CONFIRME) {
                        throw new IllegalStateException("Seul un rendez-vous CONFIRME peut être pointé en salle d'attente (statut actuel : " + rdv.getStatut() + ").");
                }

                Optional<Prestation> existante = prestationRepository.findByRendezVousId(rdv.getId());
                if (existante.isPresent()) {
                        return prestationSalonService.mapToResponse(existante.get());
                }

                List<LignePrestationDTORequest> lignesPrestation = rdv.getLignes().stream()
                                .map(l -> new LignePrestationDTORequest(l.getVarianteService().getId(), l.getPrix()))
                                .toList();

                PrestationCreateDTORequest createRequest = new PrestationCreateDTORequest(
                                rdv.getId(),
                                rdv.getCoiffeur() != null ? rdv.getCoiffeur().getId() : null,
                                rdv.getClient() != null ? rdv.getClient().getId() : null,
                                rdv.getClient() != null ? rdv.getClient().getNom() : "Client",
                                rdv.getClient() != null ? rdv.getClient().getPrenom() : "RDV",
                                rdv.getClient() != null ? rdv.getClient().getTelephone() : "",
                                StatutPrestation.EN_ATTENTE,
                                lignesPrestation);

                return prestationSalonService.creerPrestation(slugSalon, receptionnisteEmail, createRequest);
        }

        // --- HELPER METHODS ---

        private Salon verifierSalonActif(String slugSalon) {
                Salon salon = salonRepository.findBySlug(slugSalon)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Salon non trouvé pour le slug : " + slugSalon));
                if (!Boolean.TRUE.equals(salon.getStatut())) {
                        throw new IllegalArgumentException("Le salon est inactif.");
                }
                return salon;
        }

        private AffectationSalon getAffectationReceptionniste(String email, String slugSalon) {
                return affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Aucune affectation active pour la réceptionniste sur ce salon."));
        }

        private RendezVous getRendezVousSalon(String slugSalon, Long rdvId) {
                return rendezVousRepository.findByIdAndSalonSlug(rdvId, slugSalon)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Rendez-vous introuvable avec l'ID : " + rdvId));
        }


        private PlanningRendezVousDTOResponse mapToPlanningResponse(RendezVous r) {
                List<LigneRendezVousDTOResponse> lignesDTO = r.getLignes().stream()
                                .map(l -> new LigneRendezVousDTOResponse(
                                                l.getId(),
                                                l.getVarianteService().getId(),
                                                l.getVarianteService().getNom(),
                                                l.getDateHeureDebut(),
                                                l.getDateHeureFin(),
                                                l.getDuree(),
                                                l.getPrix(),
                                                l.getOrdre()))
                                .toList();

                Long coiffeurAffectationId = r.getCoiffeur() != null ? r.getCoiffeur().getId() : null;
                String coiffeurNom = (r.getCoiffeur() != null && r.getCoiffeur().getCompte() != null)
                                ? r.getCoiffeur().getCompte().getNom()
                                : null;
                String coiffeurPrenom = (r.getCoiffeur() != null && r.getCoiffeur().getCompte() != null)
                                ? r.getCoiffeur().getCompte().getPrenom()
                                : null;

                Long clientCompteId = (r.getClient() != null) ? r.getClient().getId() : null;

                return new PlanningRendezVousDTOResponse(
                                r.getId(),
                                coiffeurAffectationId,
                                coiffeurNom,
                                coiffeurPrenom,
                                r.getDateHeurePrevue(),
                                r.getDateHeureFin(),
                                r.getClient() != null ? r.getClient().getNom() : null,
                                r.getClient() != null ? r.getClient().getPrenom() : null,
                                r.getClient() != null ? r.getClient().getTelephone() : null,
                                clientCompteId,
                                r.getStatut().name(),
                                r.getMontantEstime(),
                                lignesDTO);
        }
}
