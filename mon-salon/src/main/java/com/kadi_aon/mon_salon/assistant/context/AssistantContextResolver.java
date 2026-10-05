package com.kadi_aon.mon_salon.assistant.context;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.repository.CompteRepository;
import com.kadi_aon.mon_salon.assistant.dto.AssistantChatRequest;
import com.kadi_aon.mon_salon.salon.dto.SalonDTOResponse;
import com.kadi_aon.mon_salon.salon.service.ExploreSalonService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantContextResolver {

    private final CompteRepository compteRepository;
    private final ExploreSalonService exploreSalonService;

    public AssistantContext resolve(AssistantChatRequest request, Principal principal) {
        String slug = request.getSlugSalon() != null && !request.getSlugSalon().isBlank()
                ? request.getSlugSalon().trim()
                : null;

        String email = principal != null ? principal.getName() : null;
        Compte compte = null;
        if (email != null) {
            compte = compteRepository.findByEmail(email).orElse(null);
        }

        String salonNom = null;
        if (slug != null) {
            try {
                SalonDTOResponse salon = exploreSalonService.getSalonDetail(slug);
                salonNom = salon.nom();
            } catch (Exception e) {
                log.warn("Impossible de résoudre le nom du salon pour le slug '{}': {}", slug, e.getMessage());
                salonNom = "Salon Partenaire";
            }
        }

        AssistantContextType contextType;
        if (slug != null) {
            contextType = (compte != null) ? AssistantContextType.SALON_CLIENT : AssistantContextType.SALON_VISITEUR;
        } else {
            contextType = (compte != null) ? AssistantContextType.PLATEFORME_CLIENT
                    : AssistantContextType.PLATEFORME_VISITEUR;
        }

        return AssistantContext.builder()
                .type(contextType)
                .slugSalon(slug)
                .salonNom(salonNom)
                .clientEmail(compte != null ? compte.getEmail() : null)
                .compteId(compte != null ? compte.getId() : null)
                .prenom(compte != null ? compte.getPrenom() : null)
                .nom(compte != null ? compte.getNom() : null)
                .pageCourante(request.getPageCourante())
                .ressourceCouranteId(request.getRessourceCouranteId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .maintenant(LocalDateTime.now())
                .build();
    }
}
