package com.kadi_aon.mon_salon.assistant.context;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AssistantContext {
    AssistantContextType type;
    String slugSalon;
    String salonNom;
    String clientEmail;
    Long compteId;
    String prenom;
    String nom;
    String pageCourante;
    Long ressourceCouranteId;
    Double latitude;
    Double longitude;
    LocalDateTime maintenant;

    public boolean isSalonContext() {
        return type == AssistantContextType.SALON_VISITEUR || type == AssistantContextType.SALON_CLIENT;
    }

    public boolean isAuthenticated() {
        return type == AssistantContextType.SALON_CLIENT || type == AssistantContextType.PLATEFORME_CLIENT;
    }
}
