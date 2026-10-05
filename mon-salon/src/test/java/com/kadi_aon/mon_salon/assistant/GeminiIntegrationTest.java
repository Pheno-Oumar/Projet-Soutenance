package com.kadi_aon.mon_salon.assistant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.kadi_aon.mon_salon.assistant.context.AssistantContext;
import com.kadi_aon.mon_salon.assistant.context.AssistantContextType;
import com.kadi_aon.mon_salon.assistant.dto.AssistantChatRequest;
import com.kadi_aon.mon_salon.assistant.dto.AssistantChatResponse;
import com.kadi_aon.mon_salon.assistant.service.GeminiInteractionService;

@SpringBootTest
class GeminiIntegrationTest {

    @Autowired
    private GeminiInteractionService geminiInteractionService;

    @Test
    @DisplayName("Vérifie la connexion réelle à l'API Gemini 3.8 Flash et la génération de réponse")
    void testGeminiDirectChat() {
        AssistantContext context = AssistantContext.builder()
                .type(AssistantContextType.PLATEFORME_VISITEUR)
                .maintenant(LocalDateTime.now())
                .build();

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("Bonjour, présente-toi brièvement en tant qu'assistant de la plateforme Mon Salon.")
                .sessionId("test_session_live")
                .build();

        AssistantChatResponse response = geminiInteractionService.chat(request, context);

        assertNotNull(response, "La réponse de l'assistant ne doit pas être nulle");
        assertNotNull(response.getTexte(), "Le texte de la réponse ne doit pas être nul");
        assertFalse(response.getTexte().isBlank(), "L'assistant doit fournir une réponse textuelle");
        System.out.println("=== RÉPONSE GEMINI 3.8 FLASH EN DIRECT ===");
        System.out.println(response.getTexte());
        System.out.println("=========================================");
    }

    @Test
    @DisplayName("Vérifie le Function Calling en direct avec Gemini 3.8 Flash")
    void testGeminiFunctionCalling() {
        AssistantContext context = AssistantContext.builder()
                .type(AssistantContextType.PLATEFORME_VISITEUR)
                .maintenant(LocalDateTime.now())
                .build();

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("Peux-tu rechercher des salons disponibles sur la plateforme ?")
                .sessionId("test_session_fc")
                .build();

        AssistantChatResponse response = geminiInteractionService.chat(request, context);

        assertNotNull(response);
        assertNotNull(response.getTexte());
        System.out.println("=== RÉPONSE FUNCTION CALLING EN DIRECT ===");
        System.out.println("Texte: " + response.getTexte());
        System.out.println("Nombre de cartes générées: " + (response.getCartes() != null ? response.getCartes().size() : 0));
        if (response.getCartes() != null) {
            response.getCartes().forEach(c -> System.out.println("Carte: [" + c.getType() + "] " + c.getTitre()));
        }
        System.out.println("=========================================");
    }
}
