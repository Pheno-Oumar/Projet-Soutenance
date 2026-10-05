package com.kadi_aon.mon_salon.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.notification.entity.NotificationEmail;
import com.kadi_aon.mon_salon.notification.enums.StatutNotification;

@Repository
public interface NotificationEmailRepository extends JpaRepository<NotificationEmail, Long> {

    List<NotificationEmail> findByStatut(StatutNotification statut);
}
