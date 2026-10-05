package com.kadi_aon.scheduler.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Service de génération de templates e-mail HTML conforme à la charte graphique 60-30-10 :
 * - 60% Arrière-plan : #F5F2EB (Beige Crème / Ivoire)
 * - 30% Couleur Primaire : #4A3B32 (Marron Chocolat / Brun Espresso)
 * - 10% Accents : #C8B6A6 (Marron doux poudré / Nacre) & #1A1A1A (Noir Mat)
 * - Surfaces : #FFFFFF / #FFFDFC
 * - Typographie : 'Plus Jakarta Sans', sans-serif
 */
@Service
public class HtmlEmailTemplateService {

    private static final Pattern CODE_PATTERN = Pattern.compile(
            "(?i)(?:code(?:\\s+de\\s+validation)?|code\\s*:\\s*)\\s*[:=]?\\s*([A-Za-z0-9]{4,10})"
    );

    /**
     * Génère un email HTML complet, moderne et responsive à partir du sujet et du contenu.
     */
    public String genererHtml(String sujet, String contenu) {
        if (contenu == null || contenu.isBlank()) {
            return wrapDansTemplate(sujet, "<p style='color:#7A695F;'>Aucun contenu fourni.</p>");
        }

        // Si le contenu est déjà un document HTML complet, on le préserve
        if (contenu.trim().toLowerCase().startsWith("<!doctype html") ||
            (contenu.contains("<html") && contenu.contains("</html>"))) {
            return contenu;
        }

        // Parsing et conversion dynamique en composants HTML selon la charte 60-30-10
        String bodyHtml = transformerEnHtmlRiche(sujet, contenu);
        return wrapDansTemplate(sujet, bodyHtml);
    }

    /**
     * Générateur spécifique pour la validation de compte (charte 60-30-10)
     */
    public String buildValidationCompteHtml(String nom, String prenom, String code, String boutiqueNom) {
        String civilite = (nom != null || prenom != null)
                ? ((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim()
                : "Cher utilisateur";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-bottom: 24px;'>")
          .append("<h2 style='color:#4A3B32; font-size:20px; font-weight:700; margin:0 0 10px 0;'>Bonjour ").append(escapeHtml(civilite)).append(" 👋</h2>")
          .append("<p style='color:#7A695F; font-size:15px; line-height:1.6; margin:0;'>")
          .append("Merci de créer votre compte sur la plateforme <strong>Mon Salon</strong>. Pour valider votre inscription et sécuriser votre compte, veuillez renseigner le code de validation ci-dessous.")
          .append("</p>")
          .append("</div>");

        // Bloc Code de validation aux couleurs de la charte (#F5F2EB fond, #C8B6A6 bordure tiretée, #2F251F code)
        sb.append("<div style='background: linear-gradient(145deg, #FAF8F5 0%, #F5F2EB 100%); border: 2px dashed #C8B6A6; border-radius: 14px; padding: 24px; text-align: center; margin: 26px 0; box-shadow: 0 4px 14px rgba(74, 59, 50, 0.06);'>")
          .append("<div style='font-size: 12px; font-weight: 700; color: #4A3B32; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 8px;'>🔒 Votre code de validation</div>")
          .append("<div style='font-family: Consolas, \"Courier New\", monospace; font-size: 32px; font-weight: 800; color: #2F251F; letter-spacing: 8px; margin: 6px 0;'>")
          .append(escapeHtml(code != null ? code : "------"))
          .append("</div>")
          .append("<div style='font-size: 12px; color: #7A695F; margin-top: 8px;'>Ce code est confidentiel et valable pour une durée limitée.</div>")
          .append("</div>");

        // Boutique / Salon associé
        if (boutiqueNom != null && !boutiqueNom.isBlank()) {
            sb.append("<div style='background-color:#FAF8F5; border:1px solid #EAE4DC; border-left:4px solid #C8B6A6; border-radius:10px; padding:12px 18px; margin:20px 0;'>")
              .append("<span style='color:#4A3B32; font-size:14px;'>📌 Boutique / Salon associé : <strong style='color:#2F251F;'>")
              .append(escapeHtml(boutiqueNom))
              .append("</strong></span>")
              .append("</div>");
        }

        // Conseil de sécurité
        sb.append("<div style='background:#FAF7F2; border-left:4px solid #C8B6A6; border-radius:0 8px 8px 0; padding:12px 16px; margin:22px 0; color:#7A695F; font-size:13px; line-height:1.5;'>")
          .append("🛡️ <strong style='color:#4A3B32;'>Conseil de sécurité :</strong> Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail. Votre compte ne sera pas activé sans ce code.")
          .append("</div>");

        // Signature
        sb.append("<div style='margin-top: 28px; padding-top: 18px; border-top: 1px solid #EAE4DC; color: #7A695F; font-size: 14px; line-height: 1.5;'>")
          .append("Merci et bienvenue !<br/>")
          .append("<strong style='color: #4A3B32;'>L'équipe Mon Salon</strong>")
          .append("</div>");

        return wrapDansTemplate("Validation de votre compte", sb.toString());
    }

    /**
     * Analyse le texte brut et le transforme en composants HTML selon la charte 60-30-10.
     */
    private String transformerEnHtmlRiche(String sujet, String contenu) {
        StringBuilder sb = new StringBuilder();
        String[] lignes = contenu.split("\r?\n");

        boolean inList = false;
        boolean inCredentialsBox = false;

        for (int i = 0; i < lignes.length; i++) {
            String ligne = lignes[i].trim();

            if (ligne.isEmpty()) {
                if (inList) {
                    sb.append("</ul>");
                    inList = false;
                }
                if (inCredentialsBox) {
                    sb.append("</table></div>");
                    inCredentialsBox = false;
                }
                continue;
            }

            // 1. Détection de salutation principale
            if (i == 0 && ligne.toLowerCase().startsWith("bonjour")) {
                sb.append("<h2 style='color:#4A3B32; font-size:19px; font-weight:700; margin:0 0 16px 0; line-height:1.4;'>")
                  .append(escapeHtml(ligne))
                  .append("</h2>");
                continue;
            }

            // 2. Détection d'un code de validation / code OTP
            Matcher matcher = CODE_PATTERN.matcher(ligne);
            if (matcher.find()) {
                String code = matcher.group(1);
                sb.append("<div style='background: linear-gradient(145deg, #FAF8F5 0%, #F5F2EB 100%); border: 2px dashed #C8B6A6; border-radius: 14px; padding: 22px; text-align: center; margin: 24px 0; box-shadow: 0 4px 14px rgba(74, 59, 50, 0.06);'>")
                  .append("<div style='font-size: 12px; font-weight: 700; color: #4A3B32; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 6px;'>🔒 Votre code de validation</div>")
                  .append("<div style='font-family: Consolas, \"Courier New\", monospace; font-size: 32px; font-weight: 800; color: #2F251F; letter-spacing: 8px; margin: 6px 0;'>")
                  .append(escapeHtml(code))
                  .append("</div>")
                  .append("<div style='font-size: 12px; color: #7A695F; margin-top: 6px;'>Code confidentiel à usage unique.</div>")
                  .append("</div>");
                continue;
            }

            // 3. Détection des blocs d'identifiants (ex: "- Email : ..." ou "- Mot de passe : ...")
            if (ligne.toLowerCase().contains("mot de passe") || ligne.toLowerCase().contains("identifiants de connexion") ||
               (ligne.startsWith("-") && (ligne.toLowerCase().contains("email") || ligne.toLowerCase().contains("mot de passe")))) {

                if (!inCredentialsBox && !ligne.startsWith("-")) {
                    sb.append("<div style='background:#FAF8F5; border:1px solid #EAE4DC; border-radius:12px; padding:18px 20px; margin:20px 0;'>")
                      .append("<div style='font-size:13px; font-weight:700; color:#4A3B32; margin-bottom:12px;'>🔑 ")
                      .append(escapeHtml(ligne))
                      .append("</div>")
                      .append("<table role='presentation' border='0' cellpadding='6' cellspacing='0' width='100%' style='font-size:14px;'>");
                    inCredentialsBox = true;
                    continue;
                } else if (inCredentialsBox && ligne.startsWith("-")) {
                    String[] parts = ligne.substring(1).split(":", 2);
                    if (parts.length == 2) {
                        boolean isPassword = parts[0].toLowerCase().contains("mot de passe");
                        sb.append("<tr>")
                          .append("<td style='color:#7A695F; width:45%; font-weight:500; padding:6px 0;'>").append(escapeHtml(parts[0].trim())).append(" :</td>")
                          .append("<td style='padding:6px 0;'>")
                          .append("<span style='font-family:Consolas, monospace; background:")
                          .append(isPassword ? "#F5F2EB; color:#2F251F; font-weight:700; border:1px solid #C8B6A6; padding:4px 10px; border-radius:6px; font-size:14px;'" : "#EFECE4; color:#4A3B32; font-weight:600; padding:3px 8px; border-radius:6px;'")
                          .append(">")
                          .append(escapeHtml(parts[1].trim()))
                          .append("</span></td></tr>");
                        continue;
                    }
                }
            }

            // 4. Détection d'avertissement de sécurité
            if (ligne.toLowerCase().contains("sécurité") || ligne.toLowerCase().contains("recommandons de modifier")) {
                if (inCredentialsBox) {
                    sb.append("</table></div>");
                    inCredentialsBox = false;
                }
                sb.append("<div style='background:#FAF7F2; border-left:4px solid #C8B6A6; border-radius:0 8px 8px 0; padding:12px 16px; margin:18px 0; color:#7A695F; font-size:13px; line-height:1.5;'>")
                  .append("🛡️ <strong style='color:#4A3B32;'>Conseil de sécurité :</strong> ").append(escapeHtml(ligne))
                  .append("</div>");
                continue;
            }

            // 5. Détection de salon ou boutique associée
            if (ligne.toLowerCase().contains("boutique associée") || ligne.toLowerCase().contains("salon associé")) {
                sb.append("<div style='background:#FAF8F5; border:1px solid #EAE4DC; border-left:4px solid #C8B6A6; border-radius:10px; padding:12px 18px; margin:18px 0; color:#4A3B32; font-size:14px;'>")
                  .append("🏪 <strong>").append(escapeHtml(ligne)).append("</strong>")
                  .append("</div>");
                continue;
            }

            // 6. Détection de puces / listes
            if (ligne.startsWith("-") || ligne.startsWith("•") || ligne.startsWith("*")) {
                if (inCredentialsBox) {
                    sb.append("</table></div>");
                    inCredentialsBox = false;
                }
                if (!inList) {
                    sb.append("<ul style='margin:12px 0 16px 20px; padding:0; color:#4A3B32;'>");
                    inList = true;
                }
                String text = ligne.substring(1).trim();
                sb.append("<li style='font-size:14px; line-height:1.6; margin-bottom:6px; color:#4A3B32;'>").append(escapeHtml(text)).append("</li>");
                continue;
            }

            // 7. Détection de signature finale ("Cordialement", "L'équipe...")
            if (ligne.toLowerCase().startsWith("cordialement") || ligne.toLowerCase().startsWith("l'équipe mon salon")) {
                if (inList) {
                    sb.append("</ul>");
                    inList = false;
                }
                if (inCredentialsBox) {
                    sb.append("</table></div>");
                    inCredentialsBox = false;
                }
                sb.append("<div style='margin-top:26px; padding-top:16px; border-top:1px solid #EAE4DC; color:#7A695F; font-size:14px; line-height:1.6;'>")
                  .append("Cordialement,<br/>")
                  .append("<strong style='color:#4A3B32;'>L'équipe Mon Salon</strong>")
                  .append("</div>");
                break; // fin du corps
            }

            // 8. Paragraphe standard
            if (inList) {
                sb.append("</ul>");
                inList = false;
            }
            if (inCredentialsBox) {
                sb.append("</table></div>");
                inCredentialsBox = false;
            }

            sb.append("<p style='color:#4A3B32; font-size:14px; line-height:1.65; margin:0 0 14px 0;'>")
              .append(escapeHtml(ligne))
              .append("</p>");
        }

        if (inList) {
            sb.append("</ul>");
        }
        if (inCredentialsBox) {
            sb.append("</table></div>");
        }

        return sb.toString();
    }

    /**
     * Enveloppe le corps HTML dans le gabarit complet respectant la charte 60-30-10 :
     * - 60% : Arrière-plan global #F5F2EB (Beige Crème / Ivoire)
     * - Surface : #FFFFFF (Carte pure avec bordure #EAE4DC)
     * - 30% : Header dégradé #2F251F -> #4A3B32 -> #5E4B40 (Marron Chocolat / Espresso)
     * - 10% : Accent #C8B6A6 (Nacre / Marron doux) pour badge et reflets
     * - Footer : #F5F2EB avec textes #7A695F
     */
    public String wrapDansTemplate(String sujet, String contenuHtml) {
        String titreAffiche = (sujet != null && !sujet.isBlank()) ? escapeHtml(sujet) : "Notification Mon Salon";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"fr\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "  <title>" + titreAffiche + "</title>\n" +
                "  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n" +
                "  <link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n" +
                "  <link href=\"https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap\" rel=\"stylesheet\">\n" +
                "  <style>\n" +
                "    body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }\n" +
                "    table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }\n" +
                "    img { -ms-interpolation-mode: bicubic; border: 0; outline: none; text-decoration: none; }\n" +
                "    body { height: 100% !important; margin: 0 !important; padding: 0 !important; width: 100% !important; background-color: #F5F2EB; font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; background-color: #F5F2EB; font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;\">\n" +
                "  <!-- Wrapper 60% : Arrière-plan Beige Crème / Ivoire #F5F2EB -->\n" +
                "  <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #F5F2EB; padding: 36px 12px;\">\n" +
                "    <tr>\n" +
                "      <td align=\"center\">\n" +
                "        <!-- Carte Surface #FFFFFF avec bordure subtile marron #EAE4DC et ombre douce -->\n" +
                "        <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 600px; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; box-shadow: 0 12px 30px rgba(74, 59, 50, 0.08); border: 1px solid #EAE4DC;\">\n" +
                "          <!-- Header : 30% Primaire Marron Chocolat #4A3B32 & Brun Espresso #2F251F -->\n" +
                "          <tr>\n" +
                "            <td style=\"background: linear-gradient(135deg, #2F251F 0%, #4A3B32 50%, #5E4B40 100%); padding: 36px 32px; text-align: center; border-bottom: 3px solid #C8B6A6;\">\n" +
                "              <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                <tr>\n" +
                "                  <td align=\"center\">\n" +
                "                    <!-- 10% Accent : Badge Nacre / Marron doux #C8B6A6 -->\n" +
                "                    <div style=\"display: inline-block; background: rgba(200, 182, 166, 0.22); border: 1px solid rgba(200, 182, 166, 0.55); border-radius: 30px; padding: 6px 18px; margin-bottom: 12px;\">\n" +
                "                      <span style=\"color: #C8B6A6; font-size: 13px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase;\">✨ MON SALON</span>\n" +
                "                    </div>\n" +
                "                    <h1 style=\"color: #FFFFFF; font-size: 21px; font-weight: 700; margin: 0; line-height: 1.35; letter-spacing: -0.2px;\">\n" +
                "                      " + titreAffiche + "\n" +
                "                    </h1>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "\n" +
                "          <!-- Corps de l'email : Typographie et contrastes conformes à la charte -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 36px 32px 28px 32px; background-color: #FFFFFF;\">\n" +
                "              " + contenuHtml + "\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "\n" +
                "          <!-- Footer : Beige Crème #F5F2EB, textes #7A695F et copyright #9E8E84 -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color: #F5F2EB; padding: 24px 32px; border-top: 1px solid #EAE4DC; text-align: center;\">\n" +
                "              <p style=\"margin: 0 0 8px 0; font-size: 12px; color: #7A695F; line-height: 1.5;\">\n" +
                "                🔒 <strong style=\"color: #4A3B32;\">Plateforme Mon Salon</strong> &bull; Notification automatique sécurisée.<br/>\n" +
                "                Veuillez ne pas répondre directement à ce message électronique.\n" +
                "              </p>\n" +
                "              <p style=\"margin: 0; font-size: 11px; color: #9E8E84;\">\n" +
                "                &copy; 2026 Mon Salon. Tous droits réservés.\n" +
                "              </p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
