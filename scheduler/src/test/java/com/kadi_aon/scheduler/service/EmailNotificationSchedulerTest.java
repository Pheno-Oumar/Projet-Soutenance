package com.kadi_aon.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.kadi_aon.scheduler.entity.NotificationEmail;
import com.kadi_aon.scheduler.enums.StatutNotification;
import com.kadi_aon.scheduler.repository.NotificationEmailRepository;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSchedulerTest {

    @Mock
    private NotificationEmailRepository notificationEmailRepository;

    @Mock
    private EmailWorkerService emailWorkerService;

    @InjectMocks
    private EmailNotificationScheduler emailNotificationScheduler;

    private NotificationEmail email;

    @BeforeEach
    void setUp() {
        email = NotificationEmail.builder()
                .id(1L)
                .destinataire("client@test.com")
                .sujet("Notification test")
                .contenu("Bonjour, ceci est un test.")
                .statut(StatutNotification.EN_ATTENTE)
                .build();
    }

    @Test
    void dispatcherEmailsEnAttente_succes_captureEtDelegueAuWorker() {
        when(notificationEmailRepository.findStaleEnCoursEmails(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(notificationEmailRepository.findByStatutOrderByDateCreationAsc(eq(StatutNotification.EN_ATTENTE), any(Pageable.class)))
                .thenReturn(List.of(email));
        when(notificationEmailRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emailNotificationScheduler.dispatcherEmailsEnAttente();

        assertEquals(StatutNotification.EN_COURS, email.getStatut());
        verify(notificationEmailRepository).saveAll(List.of(email));
        verify(emailWorkerService).traiterEmailAsync(1L, "client@test.com", "Notification test", "Bonjour, ceci est un test.");
    }

    @Test
    void dispatcherEmailsEnAttente_aucunEmail_neFaitRien() {
        when(notificationEmailRepository.findStaleEnCoursEmails(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(notificationEmailRepository.findByStatutOrderByDateCreationAsc(eq(StatutNotification.EN_ATTENTE), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        emailNotificationScheduler.dispatcherEmailsEnAttente();

        verify(emailWorkerService, never()).traiterEmailAsync(any(), any(), any(), any());
    }

    @Test
    void dispatcherEmailsEnAttente_recupereEmailsStale() {
        NotificationEmail staleEmail = NotificationEmail.builder()
                .id(2L)
                .destinataire("stale@test.com")
                .sujet("Stale")
                .contenu("Stale content")
                .statut(StatutNotification.EN_COURS)
                .build();

        when(notificationEmailRepository.findStaleEnCoursEmails(any(LocalDateTime.class)))
                .thenReturn(List.of(staleEmail));
        when(notificationEmailRepository.findByStatutOrderByDateCreationAsc(eq(StatutNotification.EN_ATTENTE), any(Pageable.class)))
                .thenReturn(List.of(staleEmail));
        when(notificationEmailRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emailNotificationScheduler.dispatcherEmailsEnAttente();

        assertEquals(StatutNotification.EN_COURS, staleEmail.getStatut());
        verify(emailWorkerService).traiterEmailAsync(2L, "stale@test.com", "Stale", "Stale content");
    }
}

