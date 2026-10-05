package com.kadi_aon.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HtmlEmailTemplateServiceTest {

    private HtmlEmailTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new HtmlEmailTemplateService();
    }

    @Test
    void genererHtml_texteAvecCodeValidation_genereTemplateRicheAuxCouleursCharte() {
        String sujet = "Validation de votre compte";
        String contenu = "Bonjour Amadou Diallo\n\nMerci de créer votre compte.\nVotre code de validation : 948210\nBoutique associée : Salon Élégance\nPour des raisons de sécurité, ne partagez pas ce code.\nCordialement,\nL'équipe Mon Salon";

        String html = templateService.genererHtml(sujet, contenu);

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("MON SALON"));
        assertTrue(html.contains("Plus Jakarta Sans"));
        // Vérification de la charte graphique 60-30-10
        assertTrue(html.contains("#F5F2EB"), "Doit contenir le beige crème / ivoire 60%");
        assertTrue(html.contains("#4A3B32"), "Doit contenir le marron chocolat primaire 30%");
        assertTrue(html.contains("#C8B6A6"), "Doit contenir l'accent nacre/marron doux 10%");
        assertTrue(html.contains("Validation de votre compte"));
        assertTrue(html.contains("Bonjour Amadou Diallo"));
        assertTrue(html.contains("948210"));
        assertTrue(html.contains("Salon Élégance"));
        assertTrue(html.contains("Conseil de sécurité"));
        assertTrue(html.contains("Tous droits réservés"));
    }

    @Test
    void genererHtml_texteAvecIdentifiants_genereTableauIdentifiants() {
        String sujet = "Vos accès propriétaire";
        String contenu = "Bonjour,\n\nVos identifiants de connexion :\n- Email : proprio@test.com\n- Mot de passe temporaire : Secret1234!\n\nPour des raisons de sécurité, modifiez-le vite.\nCordialement,";

        String html = templateService.genererHtml(sujet, contenu);

        assertNotNull(html);
        assertTrue(html.contains("proprio@test.com"));
        assertTrue(html.contains("Secret1234!"));
        assertTrue(html.contains("Vos identifiants de connexion"));
        assertTrue(html.contains("#4A3B32"));
        assertTrue(html.contains("#C8B6A6"));
    }

    @Test
    void buildValidationCompteHtml_methodeSpecifique_produitHtmlValideAuxCouleursCharte() {
        String html = templateService.buildValidationCompteHtml("Diallo", "Amadou", "123456", "Salon Prestige");

        assertNotNull(html);
        assertTrue(html.contains("Amadou Diallo"));
        assertTrue(html.contains("123456"));
        assertTrue(html.contains("Salon Prestige"));
        assertTrue(html.contains("MON SALON"));
        assertTrue(html.contains("#F5F2EB"));
        assertTrue(html.contains("#4A3B32"));
        assertTrue(html.contains("#C8B6A6"));
    }

    @Test
    void genererHtml_contenuDejaHtml_retourneHtmlOriginal() {
        String originalHtml = "<!DOCTYPE html><html><body><h1>Existant</h1></body></html>";
        String resultat = templateService.genererHtml("Test", originalHtml);

        assertTrue(resultat.contains("<h1>Existant</h1>"));
    }
}
