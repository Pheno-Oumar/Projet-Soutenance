package com.kadi_aon.scheduler.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.scheduler.entity.StorySalon;

@Repository
public interface StorySalonRepository extends JpaRepository<StorySalon, Long> {

    List<StorySalon> findByDateExpirationBefore(LocalDateTime now);

    List<StorySalon> findByDateExpirationBefore(LocalDateTime now, Pageable pageable);
}
