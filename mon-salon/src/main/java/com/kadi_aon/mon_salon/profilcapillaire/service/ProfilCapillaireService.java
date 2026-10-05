package com.kadi_aon.mon_salon.profilcapillaire.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;
import com.kadi_aon.mon_salon.audit.service.AuditLogService;
import com.kadi_aon.mon_salon.profilcapillaire.dto.CodeProfilDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.CodeProfilVerificationDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTORequest;
import com.kadi_aon.mon_salon.profilcapillaire.dto.ProfilCapillaireDTOResponse;
import com.kadi_aon.mon_salon.profilcapillaire.entity.ProfilCapillaire;
import com.kadi_aon.mon_salon.profilcapillaire.mapper.ProfilCapillaireDTOResponseMapper;
import com.kadi_aon.mon_salon.profilcapillaire.repository.ProfilCapillaireRepository;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;
import com.kadi_aon.mon_salon.salon.repository.AffectationSalonRepository;
import com.kadi_aon.mon_salon.salon.repository.SalonRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfilCapillaireService {

    private final ProfilCapillaireRepository profilCapillaireRepository;
    private final CompteRepository compteRepository;
    private final SalonRepository salonRepository;
    private final AffectationSalonRepository affectationSalonRepository;
    private final ProfilCapillaireDTOResponseMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public ProfilCapillaireDTOResponse getMonProfil(String clientEmail) {
        Compte compte = compteRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Compte non trouvé pour l'email : " + clientEmail));

        ProfilCapillaire profil = profilCapillaireRepository.findByCompteId(compte.getId())
                .orElseThrow(() -> new EntityNotFoundException("Aucun profil capillaire n'a été créé pour ce compte."));

        return mapper.apply(profil);
    }

    @Transactional
    public ProfilCapillaireDTOResponse enregistrerOuModifierProfil(String clientEmail, ProfilCapillaireDTORequest request) {
        Compte compte = compteRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Compte non trouvé pour l'email : " + clientEmail));

        ProfilCapillaire profil = profilCapillaireRepository.findByCompteId(compte.getId())
                .orElseGet(() -> ProfilCapillaire.builder()
                        .compte(compte)
                        .build());

        profil.setTypeCheveux(request.typeCheveux());
        profil.setTexture(request.texture());
        profil.setLongueur(request.longueur());
        profil.setDensite(request.densite());
        profil.setCuirChevelu(request.cuirChevelu());
        profil.setEtatCheveux(request.etatCheveux());
        profil.setSensibilites(request.sensibilites());
        profil.setAllergiesProduits(request.allergiesProduits());
        profil.setObservations(request.observations());

        ProfilCapillaire saved = profilCapillaireRepository.save(profil);

        auditLogService.logActionPlateforme(TypeActionAudit.MODIFICATION, "ProfilCapillaire",
                String.valueOf(saved.getId()), "ancien_profil", "nouveau_profil", compte, "CLIENT");

        log.info("Profil capillaire mis à jour avec succès pour le client {}", clientEmail);
        return mapper.apply(saved);
    }

    @Transactional
    public void definirOuChangerCodePin(String clientEmail, CodeProfilDTORequest request) {
        Compte compte = compteRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new EntityNotFoundException("Compte non trouvé pour l'email : " + clientEmail));

        ProfilCapillaire profil = profilCapillaireRepository.findByCompteId(compte.getId())
                .orElseGet(() -> ProfilCapillaire.builder()
                        .compte(compte)
                        .build());

        profil.setCodeProfil(passwordEncoder.encode(request.codePin()));
        ProfilCapillaire saved = profilCapillaireRepository.save(profil);

        auditLogService.logActionPlateforme(TypeActionAudit.CHANGEMENT_MDP, "ProfilCapillaire",
                String.valueOf(saved.getId()), "ancien_code_hash", "nouveau_code_hash", compte, "CLIENT");

        log.info("Code PIN du profil capillaire configuré avec succès pour le client {}", clientEmail);
    }

    @Transactional(readOnly = true)
    public ProfilCapillaireDTOResponse consulterProfilClientParCoiffeur(
            String slugSalon,
            Long clientCompteId,
            CodeProfilVerificationDTORequest request,
            String coiffeurEmail) {

        Salon salon = salonRepository.findBySlug(slugSalon)
                .orElseThrow(() -> new EntityNotFoundException("Salon non trouvé pour le slug : " + slugSalon));

        if (!Boolean.TRUE.equals(salon.getStatut())) {
            throw new IllegalArgumentException("Le salon est inactif.");
        }

        AffectationSalon affectationCoiffeur = affectationSalonRepository
                .findByCompteEmailAndSalonSlugAndStatutTrue(coiffeurEmail, slugSalon)
                .orElseThrow(() -> new IllegalArgumentException("Le coiffeur n'a pas d'affectation active dans ce salon."));

        ProfilCapillaire profil = profilCapillaireRepository.findByCompteId(clientCompteId)
                .orElseThrow(() -> new EntityNotFoundException("Profil capillaire introuvable pour ce client."));

        if (profil.getCodeProfil() == null || profil.getCodeProfil().isBlank()) {
            throw new IllegalArgumentException("Le client n'a pas encore configuré de code d'accès PIN pour son profil capillaire.");
        }

        if (!passwordEncoder.matches(request.codePin(), profil.getCodeProfil())) {
            throw new IllegalArgumentException("Code d'accès PIN invalide. Consultation du profil capillaire refusée.");
        }

        auditLogService.logActionSalon(TypeActionAudit.MODIFICATION, "ProfilCapillaire",
                String.valueOf(profil.getId()), "consultation_verifiee", "code_valide",
                affectationCoiffeur, "COIFFEUR");

        log.info("Profil capillaire du client ID {} consulté avec succès par le coiffeur {}", clientCompteId, coiffeurEmail);
        return mapper.apply(profil);
    }
}
