package com.kadi_aon.mon_salon.account.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.dto.ChangementMotDePasseDTORequest;
import com.kadi_aon.mon_salon.account.dto.CompteDTOResponse;
import com.kadi_aon.mon_salon.account.dto.CompteUpdateDTORequest;
import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.mapper.CompteDTOResponseMapper;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository;
    private final CompteDTOResponseMapper compteDTOResponseMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AffectationSalonRepository affectationSalonRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public CompteDTOResponse getProfil(String email) {
        Compte compte = compteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Compte non trouvé pour l'email : " + email));
        return compteDTOResponseMapper.apply(compte);
    }

    @Transactional
    public CompteDTOResponse updateProfil(
            String email,
            CompteUpdateDTORequest request,
            String slugSalon,
            String roleActif) {

        Compte compte = compteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Compte non trouvé pour l'email : " + email));

        // Règle d'unicité sur le numéro de téléphone s'il a changé
        if (!compte.getTelephone().equals(request.telephone())) {
            Optional<Compte> existingTel = compteRepository.findByTelephone(request.telephone());
            if (existingTel.isPresent() && !existingTel.get().getId().equals(compte.getId())) {
                throw new IllegalArgumentException("Ce numéro de téléphone est déjà utilisé par un autre compte.");
            }
        }

        String ancienneValeur = String.format("nom: %s, prenom: %s, telephone: %s",
                compte.getNom(), compte.getPrenom(), compte.getTelephone());

        compte.setNom(request.nom());
        compte.setPrenom(request.prenom());
        compte.setDateNaissance(request.dateNaissance());
        compte.setTelephone(request.telephone());

        Compte updated = compteRepository.save(compte);

        String nouvelleValeur = String.format("nom: %s, prenom: %s, telephone: %s",
                updated.getNom(), updated.getPrenom(), updated.getTelephone());

        // Audit de l'action selon le contexte
        logAuditForCompte(TypeActionAudit.MODIFICATION, "Compte", String.valueOf(updated.getId()),
                ancienneValeur, nouvelleValeur, email, slugSalon, roleActif);

        return compteDTOResponseMapper.apply(updated);
    }

    @Transactional
    public void changerMotDePasse(
            String email,
            ChangementMotDePasseDTORequest request,
            String slugSalon,
            String roleActif) {

        Compte compte = compteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Compte non trouvé pour l'email : " + email));

        if (!passwordEncoder.matches(request.ancienMotDePasse(), compte.getPassword())) {
            throw new IllegalArgumentException("L'ancien mot de passe fourni est incorrect.");
        }

        if (passwordEncoder.matches(request.nouveauMotDePasse(), compte.getPassword())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien mot de passe.");
        }

        compte.setPassword(passwordEncoder.encode(request.nouveauMotDePasse()));
        compteRepository.save(compte);

        // Sécurité : Révocation des sessions Refresh Token après modification du mot de passe
        refreshTokenService.revokeAllByCompte(compte);

        logAuditForCompte(TypeActionAudit.CHANGEMENT_MDP, "Compte", String.valueOf(compte.getId()),
                "ancien_mot_de_passe", "nouveau_mot_de_passe_chiffre", email, slugSalon, roleActif);

        log.info("Mot de passe mis à jour avec succès pour le compte {}", email);
    }

    private void logAuditForCompte(
            TypeActionAudit action,
            String entite,
            String entiteId,
            String ancienneVal,
            String nouvelleVal,
            String email,
            String slugSalon,
            String roleActif) {

        if (slugSalon != null && !slugSalon.isBlank()) {
            Optional<AffectationSalon> affectation =
                    affectationSalonRepository.findByCompteEmailAndSalonSlugAndStatutTrue(email, slugSalon);
            if (affectation.isPresent()) {
                auditLogService.logActionSalon(action, entite, entiteId, ancienneVal, nouvelleVal,
                        affectation.get(), roleActif != null ? roleActif : "PROPRIETAIRE");
                return;
            }
        }

        Compte compte = compteRepository.findByEmail(email).orElse(null);
        String rolePlateforme = (compte != null && compte.getRolePlateforme() != null)
                ? compte.getRolePlateforme().getRole().name() : (roleActif != null ? roleActif : "UTILISATEUR");

        auditLogService.logActionPlateforme(action, entite, entiteId, ancienneVal, nouvelleVal,
                compte, rolePlateforme);
    }
}
