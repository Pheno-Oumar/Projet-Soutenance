package com.kadi_aon.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.kadi_aon.scheduler.entity.NotificationEmail;
import com.kadi_aon.scheduler.enums.StatutNotification;
import com.kadi_aon.scheduler.repository.NotificationEmailRepository;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailWorkerServiceTest {

    @Mock
    private NotificationEmailRepository notificationEmailRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private HtmlEmailTemplateService htmlEmailTemplateService;

    @InjectMocks
    private EmailWorkerService emailWorkerService;

    private NotificationEmail email;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailWorkerService, "defaultFrom", "amadou14112004@gmail.com");
        ReflectionTestUtils.setField(emailWorkerService, "senderName", "Mon Salon");

        email = NotificationEmail.builder()
                .id(1L)
                .destinataire("client@test.com")
                .sujet("Notification test")
                .contenu("Bonjour, test worker.")
                .statut(StatutNotification.EN_COURS)
                .build();
    }

    @Test
    void traiterEmailAsync_succes() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(htmlEmailTemplateService.genererHtml(anyString(), anyString())).thenReturn("<html><body>Test HTML</body></html>");
        when(notificationEmailRepository.findById(1L)).thenReturn(Optional.of(email));

        emailWorkerService.traiterEmailAsync(1L, "client@test.com", "Notification test", "Bonjour, test worker.");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage sent = messageCaptor.getValue();
        assertEquals("client@test.com", sent.getAllRecipients()[0].toString());
        assertEquals("Notification test", sent.getSubject());

        verify(notificationEmailRepository).save(email);
        assertEquals(StatutNotification.ENVOYE, email.getStatut());
        assertNotNull(email.getDateEnvoi());
        assertNull(email.getMessageErreur());
    }

    @Test
    void traiterEmailAsync_echec_metAJourStatutEchec() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(htmlEmailTemplateService.genererHtml(anyString(), anyString())).thenReturn("<html><body>Test HTML</body></html>");
        when(notificationEmailRepository.findById(1L)).thenReturn(Optional.of(email));
        doThrow(new RuntimeException("Connexion SMTP impossible")).when(mailSender).send(any(MimeMessage.class));

        emailWorkerService.traiterEmailAsync(1L, "client@test.com", "Notification test", "Bonjour, test worker.");

        verify(notificationEmailRepository).save(email);
        assertEquals(StatutNotification.ECHEC, email.getStatut());
        assertEquals("Connexion SMTP impossible", email.getMessageErreur());
    }
}
