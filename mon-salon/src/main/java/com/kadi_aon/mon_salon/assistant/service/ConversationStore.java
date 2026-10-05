package com.kadi_aon.mon_salon.assistant.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
public class ConversationStore {

    private static final int MAX_STEPS_PER_SESSION = 20;
    private static final int TTL_MINUTES = 30;

    @Data
    public static class Step {
        private String role; // "user", "model", "function"
        private Map<String, Object> rawData;
        private LocalDateTime timestamp;

        public Step(String role, Map<String, Object> rawData) {
            this.role = role;
            this.rawData = rawData;
            this.timestamp = LocalDateTime.now();
        }
    }

    private static class SessionHistory {
        private final List<Step> steps = Collections.synchronizedList(new ArrayList<>());
        private volatile LocalDateTime lastActive = LocalDateTime.now();

        public void addStep(Step step) {
            steps.add(step);
            while (steps.size() > MAX_STEPS_PER_SESSION) {
                steps.remove(0);
            }
            lastActive = LocalDateTime.now();
        }

        public List<Step> getSteps() {
            return new ArrayList<>(steps);
        }

        public boolean isExpired() {
            return lastActive.plusMinutes(TTL_MINUTES).isBefore(LocalDateTime.now());
        }
    }

    private final Map<String, SessionHistory> sessions = new ConcurrentHashMap<>();

    public List<Step> getHistory(String sessionId) {
        if (sessionId == null) return List.of();
        cleanExpired();
        SessionHistory history = sessions.get(sessionId);
        return history != null ? history.getSteps() : List.of();
    }

    public void recordStep(String sessionId, String role, Map<String, Object> rawData) {
        if (sessionId == null) return;
        cleanExpired();
        sessions.computeIfAbsent(sessionId, k -> new SessionHistory())
                .addStep(new Step(role, rawData));
    }

    public void clear(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    private void cleanExpired() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
