package com.kadi_aon.mon_salon.rendezvous.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.coiffeur.entity.IndisponibiliteCoiffeur;
import com.kadi_aon.mon_salon.coiffeur.repository.IndisponibiliteCoiffeurRepository;
import com.kadi_aon.mon_salon.rendezvous.dto.CreneauDisponibleDTOResponse;
import com.kadi_aon.mon_salon.rendezvous.dto.DisponibiliteSearchDTORequest;
import com.kadi_aon.mon_salon.rendezvous.dto.LigneCreneauDTO;
import com.kadi_aon.mon_salon.rendezvous.entity.LigneRendezVous;
import com.kadi_aon.mon_salon.rendezvous.enums.StatutRendezVous;
import com.kadi_aon.mon_salon.rendezvous.repository.LigneRendezVousRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.HoraireOuverture;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.entity.VarianteService;
import com.kadi_aon.mon_salon.salon.enums.JourSemaine;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.FermetureExceptionnelleRepository;
import com.kadi_aon.mon_salon.salon.repository.HoraireOuvertureRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;
import com.kadi_aon.mon_salon.salon.repository.VarianteServiceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisponibiliteService {

    private final SalonRepository salonRepository;
    private final VarianteServiceRepository varianteServiceRepository;
    private final FermetureExceptionnelleRepository fermetureExceptionnelleRepository;
    private final HoraireOuvertureRepository horaireOuvertureRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final IndisponibiliteCoiffeurRepository indisponibiliteCoiffeurRepository;
    private final LigneRendezVousRepository ligneRendezVousRepository;

    private static final int PAS_MINUTES = 15;

    @Transactional(readOnly = true)
    public List<CreneauDisponibleDTOResponse> calculerDisponibilites(String slugSalon,
            DisponibiliteSearchDTORequest request) {
        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon introuvable avec le slug : " + slugSalon));

        if (!Boolean.TRUE.equals(salon.getStatut())) {
            log.warn("Salon inactif : {}", slugSalon);
            return Collections.emptyList();
        }

        LocalDate date = request.date();
        if (date.isBefore(LocalDate.now())) {
            return Collections.emptyList();
        }

        // 1. Validation stricte des variantes : statut variante ET statut du service
        // parent
        List<VarianteService> variantes = new ArrayList<>();
        int dureeTotale = 0;
        BigDecimal montantTotal = BigDecimal.ZERO;

        for (Long varId : request.varianteIds()) {
            VarianteService variante = varianteServiceRepository.findById(varId)
                    .orElseThrow(() -> new EntityNotFoundException("Prestation introuvable avec l'ID : " + varId));

            if (!variante.getServiceSalon().getSalon().getId().equals(salon.getId())) {
                throw new IllegalArgumentException(
                        "La prestation '" + variante.getNom() + "' n'appartient pas à ce salon.");
            }

            if (!Boolean.TRUE.equals(variante.getStatut())
                    || !Boolean.TRUE.equals(variante.getServiceSalon().getStatut())) {
                throw new IllegalArgumentException(
                        "La prestation '" + variante.getNom() + "' ou son service associé est actuellement inactif.");
            }

            variantes.add(variante);
            dureeTotale += variante.getDureeMinutes();
            montantTotal = montantTotal.add(variante.getPrix());
        }

        // 2. Vérification des fermetures exceptionnelles
        if (!fermetureExceptionnelleRepository.findChevauchements(salon.getId(), date, date).isEmpty()) {
            log.info("Salon fermé exceptionnellement à la date du : {}", date);
            return Collections.emptyList();
        }

        // 3. Vérification des horaires réguliers d'ouverture
        JourSemaine jourSemaine = JourSemaine.from(date.getDayOfWeek());
        HoraireOuverture horaire = horaireOuvertureRepository.findBySalonIdAndJourSemaine(salon.getId(), jourSemaine)
                .orElse(null);

        if (horaire == null) {
            log.info("Aucun horaire d'ouverture défini pour le {} (salon fermé)", jourSemaine);
            return Collections.emptyList();
        }

        LocalTime heureOuverture = horaire.getHeureOuverture();
        LocalTime heureFermeture = horaire.getHeureFermeture();
        LocalTime pauseDebut = horaire.getPauseDebut();
        LocalTime pauseFin = horaire.getPauseFin();

        // 4. Détermination de l'heure de départ du balayage (heureMinimale & heure
        // actuelle si aujourd'hui)
        LocalTime heureDepart = heureOuverture;

        if (request.heureMinimale() != null && request.heureMinimale().isAfter(heureDepart)) {
            heureDepart = request.heureMinimale();
        }

        if (date.isEqual(LocalDate.now())) {
            LocalTime nowPlusOffset = LocalTime.now().plusMinutes(15);
            if (nowPlusOffset.isAfter(heureDepart)) {
                heureDepart = nowPlusOffset;
            }
        }

        heureDepart = alignerSurPas(heureDepart, PAS_MINUTES);

        // 5. Coiffeurs éligibles actifs dans le salon
        List<AffectationSalon> coiffeursEligibles = affectationSalonRepository.findBySalonIdAndStatutTrue(salon.getId())
                .stream()
                .filter(a -> a.getRoles().stream().anyMatch(r -> r.getRole() == TypeRoleSalon.COIFFEUR))
                .toList();

        if (request.coiffeurId() != null) {
            coiffeursEligibles = coiffeursEligibles.stream()
                    .filter(c -> c.getId().equals(request.coiffeurId()))
                    .toList();

            if (coiffeursEligibles.isEmpty()) {
                throw new IllegalArgumentException("Le coiffeur demandé n'est pas disponible ou actif dans ce salon.");
            }
        }

        if (coiffeursEligibles.isEmpty()) {
            return Collections.emptyList();
        }

        // 6. Récupération des indisponibilités et RDV actifs de la journée
        LocalDateTime debutJour = date.atStartOfDay();
        LocalDateTime finJour = date.atTime(23, 59, 59);

        List<IndisponibiliteCoiffeur> indispos = indisponibiliteCoiffeurRepository
                .findBySalonIdAndPeriode(salon.getId(), debutJour, finJour);
        List<LigneRendezVous> lignesRDV = ligneRendezVousRepository.findLignesActivesBySalonAndPeriode(salon.getId(),
                debutJour, finJour, StatutRendezVous.ANNULE);

        List<CreneauDisponibleDTOResponse> resultats = new ArrayList<>();

        // 7. Balayage par pas de 15 minutes
        LocalTime courant = heureDepart;

        while (true) {
            LocalTime finGlobale = courant.plusMinutes(dureeTotale);

            // Ne doit pas dépasser la fermeture du salon
            if (finGlobale.isAfter(heureFermeture)) {
                break;
            }

            // Ne doit pas chevaucher la pause méridienne
            if (pauseDebut != null && pauseFin != null) {
                if (courant.isBefore(pauseFin) && finGlobale.isAfter(pauseDebut)) {
                    courant = courant.plusMinutes(PAS_MINUTES);
                    continue;
                }
            }

            LocalDateTime debutDT = date.atTime(courant);
            LocalDateTime finDT = date.atTime(finGlobale);

            // Mode Coiffeur Unique : on cherche un coiffeur libre sur tout l'intervalle
            // (Scénario A)
            AffectationSalon coiffeurLibre = null;
            for (AffectationSalon c : coiffeursEligibles) {
                if (isCoiffeurLibre(c.getId(), debutDT, finDT, indispos, lignesRDV)) {
                    coiffeurLibre = c;
                    break;
                }
            }

            if (coiffeurLibre != null) {
                List<LigneCreneauDTO> lignesProposees = new ArrayList<>();
                LocalTime subStart = courant;

                for (VarianteService v : variantes) {
                    LocalTime subEnd = subStart.plusMinutes(v.getDureeMinutes());
                    lignesProposees.add(new LigneCreneauDTO(
                            v.getId(),
                            v.getNom(),
                            coiffeurLibre.getId(),
                            coiffeurLibre.getCompte().getNom(),
                            coiffeurLibre.getCompte().getPrenom(),
                            subStart,
                            subEnd,
                            v.getDureeMinutes(),
                            v.getPrix()));
                    subStart = subEnd;
                }

                resultats.add(new CreneauDisponibleDTOResponse(
                        courant,
                        finGlobale,
                        dureeTotale,
                        montantTotal,
                        lignesProposees));
            }

            courant = courant.plusMinutes(PAS_MINUTES);
        }

        return resultats;
    }

    public boolean isCoiffeurLibre(
            Long coiffeurAffectationId,
            LocalDateTime debut,
            LocalDateTime fin,
            List<IndisponibiliteCoiffeur> indispos,
            List<LigneRendezVous> lignesRDV) {

        // Vérifier les indisponibilités
        for (IndisponibiliteCoiffeur ind : indispos) {
            if (ind.getCoiffeur().getId().equals(coiffeurAffectationId)) {
                if (ind.getDateDebut().isBefore(fin) && ind.getDateFin().isAfter(debut)) {
                    return false;
                }
            }
        }

        // Vérifier les rendez-vous existants (avec battement de 5 minutes)
        for (LigneRendezVous ligne : lignesRDV) {
            if (ligne.getRendezVous() != null && ligne.getRendezVous().getCoiffeur() != null &&
                    ligne.getRendezVous().getCoiffeur().getId().equals(coiffeurAffectationId)) {
                LocalDateTime finExistanteAvecTampon = ligne.getDateHeureFin().plusMinutes(5);
                if (ligne.getDateHeureDebut().isBefore(fin) && finExistanteAvecTampon.isAfter(debut)) {
                    return false;
                }
            }
        }

        return true;
    }

    private LocalTime alignerSurPas(LocalTime time, int pasMinutes) {
        int minute = time.getMinute();
        int reste = minute % pasMinutes;
        if (reste == 0 && time.getSecond() == 0) {
            return LocalTime.of(time.getHour(), minute);
        }
        LocalTime aligned = time.plusMinutes(pasMinutes - reste);
        return LocalTime.of(aligned.getHour(), aligned.getMinute());
    }
}
