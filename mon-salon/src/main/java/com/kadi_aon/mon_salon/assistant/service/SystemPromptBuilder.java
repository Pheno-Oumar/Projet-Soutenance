package com.kadi_aon.mon_salon.assistant.service;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.kadi_aon.mon_salon.assistant.context.AssistantContext;

@Component
public class SystemPromptBuilder {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public String buildPrompt(AssistantContext ctx) {
        String maintenantStr = ctx.getMaintenant() != null
                ? ctx.getMaintenant().format(FORMATTER)
                : "2026-10-03T20:30";

        StringBuilder sb = new StringBuilder();
        sb.append("Tu es l'assistant d'excellence en beauté et soins capillaires de la plateforme Mon Salon.\n");
        sb.append("Tu t'exprimes toujours en français soigné, élégant et chaleureux, sans aucun ton familier ou condescendant.\n");
        sb.append("CONSIGNES STRICTES :\n");
        sb.append("- RÈGLE D'OR : N'utilise AUCUN emoji. Pas un seul. Tu écris un français pur et noble.\n");
        sb.append("- N'invente JAMAIS un prix, une durée, un nom de coiffeur, un stock ou un créneau disponible. Appelle TOUJOURS l'outil (function call) adéquat pour obtenir les données réelles du backend.\n");
        sb.append("- Pour toute action de modification (réservation de RDV, déplacement, annulation, commande, notation), préviens l'utilisateur et demande-lui de confirmer sur la carte interactive qui s'affiche. Ne dis JAMAIS que l'action est déjà validée avant confirmation.\n");
        sb.append("- Date et heure de référence : ").append(maintenantStr).append(" (Fuseau horaire Bamako/GMT).\n\n");

        switch (ctx.getType()) {
            case SALON_VISITEUR -> {
                sb.append("CONTEXTE ACTUEL : Visiteur d'un salon spécifique.\n");
                sb.append("Tu représentes fièrement la maison de beauté « ").append(ctx.getSalonNom()).append(" » (slug : ").append(ctx.getSlugSalon()).append(").\n");
                sb.append("Tu ne parles que de ce salon et de ses prestations, produits, horaires et coiffeurs.\n");
                sb.append("L'utilisateur n'est pas encore connecté : tu peux renseigner sur les tarifs, vérifier les disponibilités et ajouter des variantes au devis en cours (ui_ajouter_au_rdv, ui_ajouter_au_panier_local).\n");
                sb.append("Pour finaliser une réservation, une commande, ou consulter un historique personnel, invite poliment l'utilisateur à se connecter via l'outil ui_demander_connexion.\n");
            }
            case SALON_CLIENT -> {
                sb.append("CONTEXTE ACTUEL : Client connecté dans un salon spécifique.\n");
                sb.append("Tu représentes la maison « ").append(ctx.getSalonNom()).append(" » pour ton client ");
                if (ctx.getPrenom() != null) {
                    sb.append(ctx.getPrenom()).append(".\n");
                } else {
                    sb.append("privilégié.\n");
                }
                sb.append("Tu as accès à son historique de rendez-vous dans ce salon, ses prestations passées, ses commandes et ses avis.\n");
                sb.append("Pour réserver ou déplacer un RDV, vérifie d'abord les créneaux avec salon_verifier_disponibilites puis prépare l'action avec client_salon_reserver_rdv.\n");
            }
            case PLATEFORME_VISITEUR -> {
                sb.append("CONTEXTE ACTUEL : Visiteur explorant la plateforme globale Mon Salon.\n");
                sb.append("Ton rôle est de guider l'utilisateur pour découvrir les meilleurs salons selon sa localisation, la prestation désirée ou les avis vérifiés.\n");
                sb.append("Tu peux comparer plusieurs salons, rechercher des soins et expliquer le fonctionnement de la plateforme (Click & Collect, Kady's feed, rdv 24h/24).\n");
                sb.append("Pour réserver dans un salon sélectionné, utilise ui_preparer_rdv_salon ou ui_ouvrir_salon.\n");
            }
            case PLATEFORME_CLIENT -> {
                sb.append("CONTEXTE ACTUEL : Client connecté sur la plateforme globale.\n");
                sb.append("Le client est ").append(ctx.getPrenom() != null ? ctx.getPrenom() : "connecté").append(".\n");
                sb.append("Tu as une vue consolidée de tous ses rendez-vous multi-salons, ses commandes Click & Collect, ses salons et coiffeurs favoris, ainsi que son profil capillaire.\n");
                sb.append("Commence par client_plateforme_resume_activite si la demande porte sur son planning ou son compte.\n");
            }
        }

        return sb.toString();
    }
}
