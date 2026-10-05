package com.kadi_aon.mon_salon.assistant.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.kadi_aon.mon_salon.assistant.dto.PendingActionDTO;

import lombok.Value;

@Service
public class PendingActionService {

    @Value
    public static class PendingAction {
        String actionId;
        String sessionId;
        String clientEmail;
        String outil;
        String resume;
        LocalDateTime expireA;
        Map<String, Object> donnees;
    }

    private final Map<String, PendingAction> storage = new ConcurrentHashMap<>();

    public PendingActionDTO createPendingAction(
            String sessionId,
            String clientEmail,
            String outil,
            String resume,
            Map<String, Object> donnees) {

        cleanExpired();

        String id = "act_" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime expireA = LocalDateTime.now().plusMinutes(10);

        PendingAction action = new PendingAction(id, sessionId, clientEmail, outil, resume, expireA, donnees);
        storage.put(id, action);

        return PendingActionDTO.builder()
                .actionId(id)
                .outil(outil)
                .resume(resume)
                .expireA(expireA)
                .donnees(donnees)
                .build();
    }

    public PendingAction getValidAction(String actionId, String sessionId, String clientEmail) {
        cleanExpired();
        PendingAction action = storage.get(actionId);
        if (action == null) {
            return null;
        }

        // Vérification session
        if (action.getSessionId() != null && !action.getSessionId().equals(sessionId)) {
            return null;
        }

        // Vérification identité si action authentifiée
        if (action.getClientEmail() != null && !action.getClientEmail().equalsIgnoreCase(clientEmail)) {
            return null;
        }

        if (action.getExpireA().isBefore(LocalDateTime.now())) {
            storage.remove(actionId);
            return null;
        }

        return action;
    }

    public void removeAction(String actionId) {
        storage.remove(actionId);
    }

    private void cleanExpired() {
        LocalDateTime now = LocalDateTime.now();
        storage.entrySet().removeIf(e -> e.getValue().getExpireA().isBefore(now));
    }
}
