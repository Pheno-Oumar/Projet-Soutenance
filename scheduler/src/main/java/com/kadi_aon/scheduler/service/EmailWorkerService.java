package com.kadi_aon.scheduler.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.scheduler.enums.StatutNotification;
import com.kadi_aon.scheduler.repository.NotificationEmailRepository;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailWorkerService {

    private final NotificationEmailRepository notificationEmailRepository;
    private final JavaMailSender mailSender;
    private final HtmlEmailTemplateService htmlEmailTemplateService;

    @Value("${app.mail.from:amadou14112004@gmail.com}")
    private String defaultFrom;

    @Value("${app.mail.sender-name:Mon Salon}")
    private String senderName;

    @Async("emailTaskExecutor")
    public void traiterEmailAsync(Long emailId, String destinataire, String sujet, String contenu) {
        log.debug("[EMAIL WORKER] Début de traitement asynchrone pour l'email #{} vers {}", emailId, destinataire);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(defaultFrom, senderName, StandardCharsets.UTF_8.name()));
            helper.setTo(destinataire);
            String sujetFinal = (sujet != null && !sujet.isBlank()) ? sujet : "Notification Mon Salon";
            helper.setSubject(sujetFinal);

            // Génération de l'interface HTML premium
            String htmlContent = htmlEmailTemplateService.genererHtml(sujetFinal, contenu);

            // Texte brut de repli et corps HTML riche
            helper.setText(contenu != null ? contenu : "", htmlContent);

            mailSender.send(message);

            mettreAJourStatut(emailId, StatutNotification.ENVOYE, null);
            log.info("[EMAIL WORKER] Email #{} envoyé avec succès via SMTP à {}", emailId, destinataire);

            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            log.error("[EMAIL WORKER] Échec d'envoi email #{} à {} : {}", emailId, destinataire, e.getMessage(), e);
            mettreAJourStatut(emailId, StatutNotification.ECHEC, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mettreAJourStatut(Long emailId, StatutNotification statut, String messageErreur) {
        notificationEmailRepository.findById(emailId).ifPresent(email -> {
            email.setStatut(statut);
            if (statut == StatutNotification.ENVOYE) {
                email.setDateEnvoi(LocalDateTime.now());
                email.setMessageErreur(null);
            } else {
                email.setMessageErreur(messageErreur);
            }
            notificationEmailRepository.save(email);
        });
    }
}
