package com.kadi_aon.mon_salon.caisse.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.caisse.dto.OperationCaisseDTOResponse;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseClotureDTORequest;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseDTOResponse;
import com.kadi_aon.mon_salon.caisse.dto.SessionCaisseOuvertureDTORequest;
import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;
import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.TypeOperationCaisse;
import com.kadi_aon.mon_salon.caisse.exception.SessionCaisseFermeeException;
import com.kadi_aon.mon_salon.caisse.repository.OperationCaisseRepository;
import com.kadi_aon.mon_salon.caisse.repository.SessionCaisseRepository;
import com.kadi_aon.mon_salon.facturation.entity.Paiement;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaisseSalonService {

    private final SessionCaisseRepository sessionCaisseRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final SalonRepository salonRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SessionCaisseDTOResponse ouvrirSessionCaisse(String slugSalon, String employeEmail, SessionCaisseOuvertureDTORequest request) {
        AffectationSalon affectation = validerComptableOuReceptionniste(slugSalon, employeEmail);

        if (sessionCaisseRepository.existsByAffectationSalonSlugAndStatut(slugSalon, StatutSessionCaisse.EN_COURS)) {
            throw new IllegalStateException("Une session de caisse est déjà en cours pour le salon " + slugSalon);
        }

        SessionCaisse session = SessionCaisse.builder()
                .affectation(affectation)
                .salon(affectation.getSalon())
                .dateOuverture(LocalDateTime.now())
                .soldeOuverture(request.soldeOuverture() != null ? request.soldeOuverture() : BigDecimal.ZERO)
                .statut(StatutSessionCaisse.EN_COURS)
                .build();

        SessionCaisse saved = sessionCaisseRepository.save(session);

        String roleName = extraireRole(affectation);
        auditLogService.logActionSalon(
                TypeActionAudit.CREATION,
                "SessionCaisse",
                saved.getId().toString(),
                null,
                "Solde ouverture: " + request.soldeOuverture(),
                affectation,
                roleName
        );

        log.info("Session de caisse {} ouverte pour le salon {} par {}", saved.getId(), slugSalon, employeEmail);
        return mapToResponse(saved);
    }

    @Transactional
    public SessionCaisseDTOResponse cloturerSessionCaisse(String slugSalon, String employeEmail, SessionCaisseClotureDTORequest request) {
        AffectationSalon affectation = validerComptableOuReceptionniste(slugSalon, employeEmail);

        SessionCaisse session = sessionCaisseRepository.findByAffectationSalonSlugAndStatut(slugSalon, StatutSessionCaisse.EN_COURS)
                .orElseThrow(() -> new IllegalStateException("Aucune session de caisse active trouvée à clôturer pour le salon " + slugSalon));

        BigDecimal soldeFermeture = (request != null && request.soldeFermeture() != null)
                ? request.soldeFermeture()
                : calculerSoldeTheorique(session);

        session.setDateCloture(LocalDateTime.now());
        session.setSoldeFermeture(soldeFermeture);
        session.setStatut(StatutSessionCaisse.CLOTURE);

        SessionCaisse saved = sessionCaisseRepository.save(session);

        String roleName = extraireRole(affectation);
        auditLogService.logActionSalon(
                TypeActionAudit.MODIFICATION,
                "SessionCaisse",
                saved.getId().toString(),
                "EN_COURS",
                "Solde cloture: " + soldeFermeture,
                affectation,
                roleName
        );

        log.info("Session de caisse {} clôturée pour le salon {} par {}", saved.getId(), slugSalon, employeEmail);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public SessionCaisseDTOResponse obtenirSessionCourante(String slugSalon) {
        SessionCaisse session = sessionCaisseRepository.findByAffectationSalonSlugAndStatut(slugSalon, StatutSessionCaisse.EN_COURS)
                .orElseThrow(() -> new SessionCaisseFermeeException("Aucune session de caisse n'est actuellement ouverte pour le salon " + slugSalon));

        return mapToResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionCaisseDTOResponse> listerHistoriqueSessions(String slugSalon) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }
        return sessionCaisseRepository.findByAffectationSalonSlugOrderByDateOuvertureDesc(slugSalon).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperationCaisseDTOResponse> listerOperationsSession(String slugSalon, Long sessionId) {
        if (!salonRepository.existsBySlug(slugSalon)) {
            throw new EntityNotFoundException("Salon introuvable pour le slug : " + slugSalon);
        }
        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session de caisse introuvable ID : " + sessionId));

        if (!session.getAffectation().getSalon().getSlug().equals(slugSalon)) {
            throw new IllegalArgumentException("La session de caisse #" + sessionId + " n'appartient pas au salon " + slugSalon);
        }

        return operationCaisseRepository.findBySessionCaisseIdOrderByDateOperationDesc(sessionId).stream()
                .map(o -> new OperationCaisseDTOResponse(
                        o.getId(),
                        o.getMontant(),
                        o.getDateOperation(),
                        o.getLibelle(),
                        o.getType().name(),
                        o.getStatut(),
                        (o.getPaiement() != null) ? o.getPaiement().getNumeroPaiement() : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionCaisse obtenirSessionActive(String slugSalon) {
        return sessionCaisseRepository.findByAffectationSalonSlugAndStatut(slugSalon, StatutSessionCaisse.EN_COURS)
                .orElseThrow(() -> new SessionCaisseFermeeException("Impossible d'effectuer l'opération financière : aucune session de caisse n'est ouverte pour le salon " + slugSalon));
    }


    @Transactional
    public OperationCaisse enregistrerOperationEntree(SessionCaisse session, BigDecimal montant, String libelle, Paiement paiement) {
        OperationCaisse op = OperationCaisse.builder()
                .sessionCaisse(session)
                .montant(montant)
                .dateOperation(LocalDateTime.now())
                .libelle(libelle)
                .type(TypeOperationCaisse.ENTREE)
                .statut(true)
                .paiement(paiement)
                .build();
        return operationCaisseRepository.save(op);
    }

    @Transactional
    public OperationCaisse enregistrerOperationSortie(SessionCaisse session, BigDecimal montant, String libelle, Paiement paiement) {
        OperationCaisse op = OperationCaisse.builder()
                .sessionCaisse(session)
                .montant(montant)
                .dateOperation(LocalDateTime.now())
                .libelle(libelle)
                .type(TypeOperationCaisse.SORTIE)
                .statut(true)
                .paiement(paiement)
                .build();
        return operationCaisseRepository.save(op);
    }

    public BigDecimal calculerSoldeTheorique(SessionCaisse s) {
        List<OperationCaisse> ops = s.getOperations() != null ? s.getOperations() : List.of();
        BigDecimal totalEntrees = ops.stream()
                .filter(o -> o.getType() == TypeOperationCaisse.ENTREE && Boolean.TRUE.equals(o.getStatut()))
                .map(OperationCaisse::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSorties = ops.stream()
                .filter(o -> o.getType() == TypeOperationCaisse.SORTIE && Boolean.TRUE.equals(o.getStatut()))
                .map(OperationCaisse::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ouverture = s.getSoldeOuverture() != null ? s.getSoldeOuverture() : BigDecimal.ZERO;
        return ouverture.add(totalEntrees).subtract(totalSorties);
    }

    private AffectationSalon validerComptableOuReceptionniste(String slugSalon, String email) {
        AffectationSalon affectation = affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour l'utilisateur " + email + " sur le salon " + slugSalon));

        boolean hasRole = affectation.getRoles().stream()
                .anyMatch(r -> r.getRole() == TypeRoleSalon.COMPTABLE || r.getRole() == TypeRoleSalon.RECEPTIONNISTE);

        if (!hasRole) {
            throw new IllegalArgumentException("L'utilisateur " + email + " ne possède ni le rôle COMPTABLE ni le rôle RECEPTIONNISTE sur le salon " + slugSalon);
        }
        return affectation;
    }

    private String extraireRole(AffectationSalon affectation) {
        return affectation.getRoles().stream()
                .map(r -> r.getRole().name())
                .filter(r -> r.equals("COMPTABLE") || r.equals("RECEPTIONNISTE"))
                .findFirst()
                .orElse("EMPLOYE");
    }

    private SessionCaisseDTOResponse mapToResponse(SessionCaisse s) {
        List<OperationCaisse> ops = s.getOperations() != null ? s.getOperations() : List.of();
        BigDecimal totalEntrees = ops.stream()
                .filter(o -> o.getType() == TypeOperationCaisse.ENTREE && Boolean.TRUE.equals(o.getStatut()))
                .map(OperationCaisse::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSorties = ops.stream()
                .filter(o -> o.getType() == TypeOperationCaisse.SORTIE && Boolean.TRUE.equals(o.getStatut()))
                .map(OperationCaisse::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ouverture = s.getSoldeOuverture() != null ? s.getSoldeOuverture() : BigDecimal.ZERO;
        BigDecimal soldeTheorique = ouverture.add(totalEntrees).subtract(totalSorties);

        List<OperationCaisseDTOResponse> opsDTO = ops.stream()
                .map(o -> new OperationCaisseDTOResponse(
                        o.getId(),
                        o.getMontant(),
                        o.getDateOperation(),
                        o.getLibelle(),
                        o.getType().name(),
                        o.getStatut(),
                        (o.getPaiement() != null) ? o.getPaiement().getNumeroPaiement() : null
                ))
                .toList();

        return new SessionCaisseDTOResponse(
                s.getId(),
                s.getAffectation().getId(),
                s.getAffectation().getCompte().getNom(),
                s.getAffectation().getCompte().getPrenom(),
                s.getAffectation().getCompte().getEmail(),
                s.getDateOuverture(),
                s.getDateCloture(),
                s.getSoldeOuverture(),
                s.getSoldeFermeture(),
                totalEntrees,
                totalSorties,
                soldeTheorique,
                s.getStatut().name(),
                opsDTO
        );
    }
}
