package com.kadi_aon.mon_salon.coiffeur.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.avis.entity.AvisPrestation;
import com.kadi_aon.mon_salon.avis.repository.AvisPrestationRepository;
import com.kadi_aon.mon_salon.coiffeur.dto.CoiffeurDashboardDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.IndisponibiliteDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTORequest;
import com.kadi_aon.mon_salon.coiffeur.dto.ProfilCoiffeurDTOResponse;
import com.kadi_aon.mon_salon.coiffeur.entity.IndisponibiliteCoiffeur;
import com.kadi_aon.mon_salon.coiffeur.entity.ProfilCoiffeur;
import com.kadi_aon.mon_salon.coiffeur.mapper.IndisponibiliteDTOResponseMapper;
import com.kadi_aon.mon_salon.coiffeur.mapper.ProfilCoiffeurDTOResponseMapper;
import com.kadi_aon.mon_salon.coiffeur.repository.IndisponibiliteCoiffeurRepository;
import com.kadi_aon.mon_salon.coiffeur.repository.ProfilCoiffeurRepository;
import com.kadi_aon.mon_salon.prestation.enums.StatutPrestation;
import com.kadi_aon.mon_salon.prestation.repository.PrestationRepository;
import com.kadi_aon.mon_salon.profilcapillaire.repository.ProfilCapillaireRepository;
import com.kadi_aon.mon_salon.rendezvous.dto.PlanningRendezVousDTOResponse;
import com.kadi_aon.mon_salon.salon.dto.pilotage.ClientSalonResumeDTOResponse;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;
import com.kadi_aon.mon_salon.common.service.CloudinaryService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoiffeurSalonService {

    private final AffectationSalonRepository affectationSalonRepository;
    private final ProfilCoiffeurRepository profilCoiffeurRepository;
    private final IndisponibiliteCoiffeurRepository indisponibiliteCoiffeurRepository;
    private final ProfilCoiffeurDTOResponseMapper profilCoiffeurDTOResponseMapper;
    private final IndisponibiliteDTOResponseMapper indisponibiliteDTOResponseMapper;
    private final AuditLogService auditLogService;
    private final CloudinaryService cloudinaryService;
    private final com.kadi_aon.mon_salon.salon.service.ReceptionnisteSalonService receptionnisteSalonService;
    private final PrestationRepository prestationRepository;
    private final AvisPrestationRepository avisPrestationRepository;
    private final ProfilCapillaireRepository profilCapillaireRepository;
    private final com.kadi_aon.mon_salon.salon.repository.SalonRepository salonRepository;

    @Transactional(readOnly = true)
    public List<ProfilCoiffeurDTOResponse> listerCoiffeursVitrine(String slugSalon) {
        com.kadi_aon.mon_salon.salon.entity.Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        if (!Boolean.TRUE.equals(salon.getStatut())) {
            return java.util.Collections.emptyList();
        }

        List<AffectationSalon> coiffeurs = affectationSalonRepository.findBySalonIdAndStatutTrue(salon.getId()).stream()
                .filter(a -> a.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR))
                .toList();

        return coiffeurs.stream()
                .map(this::getOrCreateProfil)
                .map(profilCoiffeurDTOResponseMapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public CoiffeurDashboardDTOResponse obtenirDashboardCoiffeur(String slugSalon, String email) {
        AffectationSalon affectation = getAffectationCoiffeurValide(slugSalon, email);
        ProfilCoiffeur profil = getOrCreateProfil(affectation);

        LocalDate today = LocalDate.now();
        List<PlanningRendezVousDTOResponse> planningAujourdhui = receptionnisteSalonService
                .consulterPlanning(slugSalon, today, affectation.getId());

        long rdvAujourdhuiTotal = planningAujourdhui.size();
        long rdvAujourdhuiTermines = planningAujourdhui.stream()
                .filter(p -> "TERMINE".equalsIgnoreCase(p.statut()))
                .count();
        long rdvAujourdhuiEnAttente = planningAujourdhui.stream()
                .filter(p -> !"TERMINE".equalsIgnoreCase(p.statut())
                        && !"ANNULE".equalsIgnoreCase(p.statut())
                        && !"NO_SHOW".equalsIgnoreCase(p.statut()))
                .count();

        LocalDateTime debutMois = today.withDayOfMonth(1).atStartOfDay();
        long prestationsMoisTerminees = prestationRepository.countPrestationsMoisCoiffeur(
                affectation.getId(), slugSalon, StatutPrestation.TERMINEE, debutMois);

        List<AvisPrestation> avisList = avisPrestationRepository
                .findByCoiffeurEmailAndSalonSlugAndStatut(email, slugSalon, true);
        double noteMoyenneBrute = avisList.isEmpty() ? 0.0 : avisList.stream().mapToInt(AvisPrestation::getNote).average().orElse(0.0);
        double noteMoyenne = Math.round(noteMoyenneBrute * 10.0) / 10.0;
        long totalAvis = avisList.size();

        LocalDateTime now = LocalDateTime.now();
        List<IndisponibiliteDTOResponse> prochainesIndisponibilites = indisponibiliteCoiffeurRepository
                .findByCoiffeurIdOrderByDateDebutAsc(affectation.getId())
                .stream()
                .filter(i -> i.getDateFin().isAfter(now))
                .limit(5)
                .map(indisponibiliteDTOResponseMapper)
                .toList();

        return CoiffeurDashboardDTOResponse.builder()
                .affectationId(affectation.getId())
                .nomAffichage(profil.getNomAffichage() != null ? profil.getNomAffichage() : (affectation.getCompte().getPrenom() + " " + affectation.getCompte().getNom()))
                .biographie(profil.getBiographie())
                .anneeExperience(profil.getAnneeExperience())
                .photoProfilUrl(profil.getPhotoProfilUrl())
                .rdvAujourdhuiTotal(rdvAujourdhuiTotal)
                .rdvAujourdhuiTermines(rdvAujourdhuiTermines)
                .rdvAujourdhuiEnAttente(rdvAujourdhuiEnAttente)
                .prestationsMoisTerminees(prestationsMoisTerminees)
                .noteMoyenne(noteMoyenne)
                .totalAvis(totalAvis)
                .planningAujourdhui(planningAujourdhui)
                .prochainesIndisponibilites(prochainesIndisponibilites)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ClientSalonResumeDTOResponse> listerClientsPourCoiffeur(String slugSalon, String query, String email) {
        getAffectationCoiffeurValide(slugSalon, email);

        List<Compte> clients = affectationSalonRepository.findComptesBySalonSlugAndRole(slugSalon, TypeRoleSalon.CLIENT);

        String searchQuery = (query != null) ? query.trim().toLowerCase() : null;

        return clients.stream()
                .filter(c -> {
                    if (searchQuery == null || searchQuery.isBlank()) {
                        return true;
                    }
                    String nom = c.getNom() != null ? c.getNom().toLowerCase() : "";
                    String prenom = c.getPrenom() != null ? c.getPrenom().toLowerCase() : "";
                    String tel = c.getTelephone() != null ? c.getTelephone().toLowerCase() : "";
                    String mail = c.getEmail() != null ? c.getEmail().toLowerCase() : "";
                    return nom.contains(searchQuery) || prenom.contains(searchQuery) || tel.contains(searchQuery) || mail.contains(searchQuery);
                })
                .map(c -> ClientSalonResumeDTOResponse.builder()
                        .clientId(c.getId())
                        .nom(c.getNom())
                        .prenom(c.getPrenom())
                        .email(c.getEmail())
                        .telephone(c.getTelephone())
                        .dateNaissance(c.getDateNaissance())
                        .dateInscription(c.getDateCreation())
                        .hasProfilCapillaire(profilCapillaireRepository.existsByCompteId(c.getId()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanningRendezVousDTOResponse> obtenirPlanningCoiffeur(
            String slugSalon, String email, LocalDate date) {
        AffectationSalon affectation = getAffectationCoiffeurValide(slugSalon, email);
        return receptionnisteSalonService.consulterPlanning(slugSalon, date, affectation.getId());
    }

    @Transactional(readOnly = true)
    public ProfilCoiffeurDTOResponse getProfil(String slugSalon, String email) {
        AffectationSalon affectation = getAffectationCoiffeurValide(slugSalon, email);

        ProfilCoiffeur profil = profilCoiffeurRepository.findByAffectationId(affectation.getId())
                .orElseGet(() -> ProfilCoiffeur.builder()
                        .affectation(affectation)
                        .nomAffichage(affectation.getCompte().getPrenom() + " " + affectation.getCompte().getNom())
                        .build());

        return profilCoiffeurDTOResponseMapper.apply(profil);
    }

    @Transactional
    public ProfilCoiffeurDTOResponse updateProfil(String slugSalon, ProfilCoiffeurDTORequest request, String email) {
        AffectationSalon affectation = getAffectationCoiffeurValide(slugSalon, email);

        ProfilCoiffeur profil = profilCoiffeurRepository.findByAffectationId(affectation.getId())
                .orElseGet(() -> ProfilCoiffeur.builder().affectation(affectation).build());

        String ancienneValeur = String.format("nom=%s, exp=%s", profil.getNomAffichage(), profil.getAnneeExperience());

        if (request.nomAffichage() != null) {
            profil.setNomAffichage(request.nomAffichage().trim());
        }
        if (request.biographie() != null) {
            profil.setBiographie(request.biographie().trim());
        }
        if (request.anneeExperience() != null) {
            profil.setAnneeExperience(request.anneeExperience());
        }
        if (request.photoProfilUrl() != null) {
            profil.setPhotoProfilUrl(request.photoProfilUrl().trim());
        }
        if (request.description() != null) {
            profil.setDescription(request.description().trim());
        }

        ProfilCoiffeur saved = profilCoiffeurRepository.save(profil);

        String nouvelleValeur = String.format("nom=%s, exp=%s", saved.getNomAffichage(), saved.getAnneeExperience());
        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "ProfilCoiffeur",
                String.valueOf(saved.getId()),
                ancienneValeur,
                nouvelleValeur,
                affectation,
                "COIFFEUR"
        );

        return profilCoiffeurDTOResponseMapper.apply(saved);
    }

    @Transactional
    public IndisponibiliteDTOResponse ajouterIndisponibilite(String slugSalon, IndisponibiliteDTORequest request, String email) {
        AffectationSalon affectation = getAffectationCoiffeurValide(slugSalon, email);

        if (request.dateDebut() == null || request.dateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (!request.dateDebut().isBefore(request.dateFin())) {
            throw new IllegalArgumentException("La date de début doit être strictement antérieure à la date de fin.");
        }

        // Vérification de chevauchement avec une autre indisponibilité existante pour ce coiffeur
        List<IndisponibiliteCoiffeur> chevauchements = indisponibiliteCoiffeurRepository.findByCoiffeurIdAndPeriode(
                affectation.getId(),
                request.dateDebut(),
                request.dateFin()
        );

        if (!chevauchements.isEmpty()) {
            throw new IllegalArgumentException("Une période d'indisponibilité chevauche déjà cet intervalle pour ce coiffeur.");
        }

        IndisponibiliteCoiffeur entity = IndisponibiliteCoiffeur.builder()
                .coiffeur(affectation)
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .motif(request.motif())
                .commentaire(request.commentaire())
                .build();

        IndisponibiliteCoiffeur saved = indisponibiliteCoiffeurRepository.save(entity);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "IndisponibiliteCoiffeur",
                String.valueOf(saved.getId()),
                null,
                String.format("Motif=%s, du=%s au=%s", saved.getMotif(), saved.getDateDebut(), saved.getDateFin()),
                affectation,
                "COIFFEUR"
        );

        return indisponibiliteDTOResponseMapper.apply(saved);
    }

    @Transactional(readOnly = true)
    public List<IndisponibiliteDTOResponse> listerMesIndisponibilites(String slugSalon, String email) {
        AffectationSalon affectation = getAffectationCoiffeurValide(slugSalon, email);

        return indisponibiliteCoiffeurRepository.findByCoiffeurIdOrderByDateDebutAsc(affectation.getId())
                .stream()
                .map(indisponibiliteDTOResponseMapper)
                .toList();
    }

    @Transactional
    public void supprimerIndisponibilite(String slugSalon, Long indisponibiliteId, String email) {
        AffectationSalon affectation = getAffectationCoiffeurValide(slugSalon, email);

        IndisponibiliteCoiffeur indisponibilite = indisponibiliteCoiffeurRepository.findById(indisponibiliteId)
                .orElseThrow(() -> new EntityNotFoundException("Indisponibilité introuvable avec l'ID : " + indisponibiliteId));

        if (!indisponibilite.getCoiffeur().getId().equals(affectation.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer l'indisponibilité d'un autre coiffeur.");
        }

        if (!indisponibilite.getDateDebut().isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("Une indisponibilité ne peut être supprimée qu'avant sa date de début.");
        }

        indisponibiliteCoiffeurRepository.delete(indisponibilite);

        auditLogService.logActionSalon(
                TypeActionAudit.SUPPRESSION,
                "IndisponibiliteCoiffeur",
                String.valueOf(indisponibiliteId),
                String.format("du=%s au=%s", indisponibilite.getDateDebut(), indisponibilite.getDateFin()),
                "SUPPRIMEE",
                affectation,
                "COIFFEUR"
        );
    }

    public AffectationSalon getAffectationManagerOuProprietaireValide(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Aucune affectation active trouvée pour ce salon."));

        boolean isManagerOuProprio = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.MANAGER || r.getRole() == TypeRoleSalon.PROPRIETAIRE);

        if (!isManagerOuProprio) {
            throw new IllegalArgumentException("Seul le manager ou le propriétaire peut gérer les indisponibilités.");
        }

        return affectation;
    }

    @Transactional
    public IndisponibiliteDTOResponse ajouterIndisponibiliteParManagerOuProprio(
            String slugSalon, IndisponibiliteDTORequest request, String auteurEmail) {
        AffectationSalon auteur = getAffectationManagerOuProprietaireValide(slugSalon, auteurEmail);

        if (request.coiffeurAffectationId() == null) {
            throw new IllegalArgumentException("L'identifiant du coiffeur (coiffeurAffectationId) est obligatoire.");
        }

        AffectationSalon coiffeur = affectationSalonRepository.findById(request.coiffeurAffectationId())
                .orElseThrow(() -> new EntityNotFoundException("Coiffeur introuvable ID " + request.coiffeurAffectationId()));

        if (!coiffeur.getSalon().getSlug().equals(slugSalon) || !Boolean.TRUE.equals(coiffeur.getStatut())) {
            throw new IllegalArgumentException("Ce coiffeur n'appartient pas au salon spécifié ou est inactif.");
        }

        boolean isCoiffeur = coiffeur.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR);
        if (!isCoiffeur) {
            throw new IllegalArgumentException("L'employé sélectionné n'a pas le rôle COIFFEUR.");
        }

        if (request.dateDebut() == null || request.dateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (!request.dateDebut().isBefore(request.dateFin())) {
            throw new IllegalArgumentException("La date de début doit être strictement antérieure à la date de fin.");
        }
        if (!request.dateDebut().isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Une indisponibilité doit être planifiée dans le futur.");
        }

        List<IndisponibiliteCoiffeur> chevauchements = indisponibiliteCoiffeurRepository.findByCoiffeurIdAndPeriode(
                coiffeur.getId(), request.dateDebut(), request.dateFin()
        );
        if (!chevauchements.isEmpty()) {
            throw new IllegalArgumentException("Une période d'indisponibilité chevauche déjà cet intervalle pour ce coiffeur.");
        }

        IndisponibiliteCoiffeur entity = IndisponibiliteCoiffeur.builder()
                .coiffeur(coiffeur)
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .motif(request.motif())
                .commentaire(request.commentaire())
                .build();

        IndisponibiliteCoiffeur saved = indisponibiliteCoiffeurRepository.save(entity);

        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "IndisponibiliteCoiffeur",
                String.valueOf(saved.getId()),
                null,
                String.format("Coiffeur=%s, Motif=%s, du=%s au=%s", coiffeur.getCompte().getEmail(), saved.getMotif(), saved.getDateDebut(), saved.getDateFin()),
                auteur,
                auteur.getRoles().iterator().next().getRole().name()
        );

        return indisponibiliteDTOResponseMapper.apply(saved);
    }

    @Transactional
    public IndisponibiliteDTOResponse modifierIndisponibiliteParManagerOuProprio(
            String slugSalon, Long indisponibiliteId, IndisponibiliteDTORequest request, String auteurEmail) {
        AffectationSalon auteur = getAffectationManagerOuProprietaireValide(slugSalon, auteurEmail);

        IndisponibiliteCoiffeur indisponibilite = indisponibiliteCoiffeurRepository.findByIdAndCoiffeurSalonSlug(indisponibiliteId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Indisponibilité introuvable ID " + indisponibiliteId));

        if (!indisponibilite.getDateDebut().isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("Une indisponibilité ne peut être modifiée qu'avant sa date de début.");
        }

        if (request.dateDebut() == null || request.dateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (!request.dateDebut().isBefore(request.dateFin())) {
            throw new IllegalArgumentException("La date de début doit être strictement antérieure à la date de fin.");
        }
        if (!request.dateDebut().isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("La date de début doit être dans le futur.");
        }

        List<IndisponibiliteCoiffeur> chevauchements = indisponibiliteCoiffeurRepository.findByCoiffeurIdAndPeriode(
                indisponibilite.getCoiffeur().getId(), request.dateDebut(), request.dateFin()
        ).stream().filter(i -> !i.getId().equals(indisponibiliteId)).toList();

        if (!chevauchements.isEmpty()) {
            throw new IllegalArgumentException("Une autre indisponibilité chevauche déjà cette période.");
        }

        indisponibilite.setDateDebut(request.dateDebut());
        indisponibilite.setDateFin(request.dateFin());
        if (request.motif() != null) {
            indisponibilite.setMotif(request.motif());
        }
        if (request.commentaire() != null) {
            indisponibilite.setCommentaire(request.commentaire());
        }

        IndisponibiliteCoiffeur saved = indisponibiliteCoiffeurRepository.save(indisponibilite);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "IndisponibiliteCoiffeur",
                String.valueOf(saved.getId()),
                null,
                String.format("Modifiée : du=%s au=%s", saved.getDateDebut(), saved.getDateFin()),
                auteur,
                auteur.getRoles().iterator().next().getRole().name()
        );

        return indisponibiliteDTOResponseMapper.apply(saved);
    }

    @Transactional
    public void supprimerIndisponibiliteParManagerOuProprio(String slugSalon, Long indisponibiliteId, String auteurEmail) {
        AffectationSalon auteur = getAffectationManagerOuProprietaireValide(slugSalon, auteurEmail);

        IndisponibiliteCoiffeur indisponibilite = indisponibiliteCoiffeurRepository.findByIdAndCoiffeurSalonSlug(indisponibiliteId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Indisponibilité introuvable ID " + indisponibiliteId));

        if (!indisponibilite.getDateDebut().isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("Une indisponibilité ne peut être supprimée qu'avant sa date de début.");
        }

        indisponibiliteCoiffeurRepository.delete(indisponibilite);

        auditLogService.logActionSalon(
                TypeActionAudit.SUPPRESSION,
                "IndisponibiliteCoiffeur",
                String.valueOf(indisponibiliteId),
                String.format("du=%s au=%s", indisponibilite.getDateDebut(), indisponibilite.getDateFin()),
                "SUPPRIMEE",
                auteur,
                auteur.getRoles().iterator().next().getRole().name()
        );
    }

    @Transactional
    public IndisponibiliteDTOResponse mettreFinIndisponibiliteParManagerOuProprio(
            String slugSalon, Long indisponibiliteId, String auteurEmail) {
        AffectationSalon auteur = getAffectationManagerOuProprietaireValide(slugSalon, auteurEmail);

        IndisponibiliteCoiffeur indisponibilite = indisponibiliteCoiffeurRepository.findByIdAndCoiffeurSalonSlug(indisponibiliteId, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Indisponibilité introuvable ID " + indisponibiliteId));

        LocalDateTime now = java.time.LocalDateTime.now();

        if (indisponibilite.getDateFin().isBefore(now)) {
            throw new IllegalStateException("Cette indisponibilité est déjà terminée.");
        }

        if (indisponibilite.getDateDebut().isAfter(now)) {
            throw new IllegalStateException("Cette indisponibilité n'a pas encore commencé. Vous pouvez la supprimer ou la modifier.");
        }

        LocalDateTime ancienneDateFin = indisponibilite.getDateFin();
        indisponibilite.setDateFin(now);
        IndisponibiliteCoiffeur saved = indisponibiliteCoiffeurRepository.save(indisponibilite);

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "IndisponibiliteCoiffeur",
                String.valueOf(indisponibiliteId),
                "dateFin: " + ancienneDateFin,
                "dateFin: " + now + " (mise fin anticipée)",
                auteur,
                auteur.getRoles().iterator().next().getRole().name()
        );

        return indisponibiliteDTOResponseMapper.apply(saved);
    }

    @Transactional(readOnly = true)
    public List<IndisponibiliteDTOResponse> listerIndisponibilitesSalon(String slugSalon, Long coiffeurAffectationId, String auteurEmail) {
        getAffectationManagerOuProprietaireValide(slugSalon, auteurEmail);

        List<IndisponibiliteCoiffeur> list;
        if (coiffeurAffectationId != null) {
            list = indisponibiliteCoiffeurRepository.findByCoiffeurIdOrderByDateDebutAsc(coiffeurAffectationId);
        } else {
            list = indisponibiliteCoiffeurRepository.findByCoiffeurSalonSlugOrderByDateDebutAsc(slugSalon);
        }

        return list.stream().map(indisponibiliteDTOResponseMapper).toList();
    }

    private ProfilCoiffeur getOrCreateProfil(AffectationSalon affectation) {
        return profilCoiffeurRepository.findByAffectationId(affectation.getId())
                .orElseGet(() -> ProfilCoiffeur.builder()
                        .affectation(affectation)
                        .nomAffichage(affectation.getCompte().getPrenom() + " " + affectation.getCompte().getNom())
                        .build());
    }

    @Transactional
    public ProfilCoiffeurDTOResponse uploadPhotoProfil(String slugSalon, MultipartFile file, String email) throws IOException {
        AffectationSalon affectation = getAffectationCoiffeurValide(slugSalon, email);
        ProfilCoiffeur profil = getOrCreateProfil(affectation);
        String anciennePhoto = profil.getPhotoProfilUrl();

        String webpUrl = cloudinaryService.uploadPhotoProfilCoiffeur(file, slugSalon, affectation.getCompte().getId());
        profil.setPhotoProfilUrl(webpUrl);

        ProfilCoiffeur saved = profilCoiffeurRepository.save(profil);

        // Supprimer l'ancienne photo Cloudinary si existante
        if (anciennePhoto != null && !anciennePhoto.isBlank()) {
            cloudinaryService.deleteMediaByUrl(anciennePhoto, "image");
        }

        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "ProfilCoiffeur (Photo)",
                String.valueOf(saved.getId()),
                anciennePhoto != null ? anciennePhoto : "aucune",
                webpUrl,
                affectation,
                "COIFFEUR"
        );

        return profilCoiffeurDTOResponseMapper.apply(saved);
    }

    public AffectationSalon getAffectationCoiffeurValide(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Aucune affectation active trouvée pour ce salon."));

        boolean isCoiffeur = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR);

        if (!isCoiffeur) {
            throw new IllegalArgumentException("Vous ne possédez pas le rôle COIFFEUR dans ce salon.");
        }

        return affectation;
    }
}
