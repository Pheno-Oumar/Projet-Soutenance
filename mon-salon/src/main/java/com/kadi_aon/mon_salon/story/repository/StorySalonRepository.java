package com.kadi_aon.mon_salon.story.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.story.entity.StorySalon;

@Repository
public interface StorySalonRepository extends JpaRepository<StorySalon, Long> {

    @Query("SELECT s FROM StorySalon s JOIN FETCH s.salon WHERE s.salon.slug = :slugSalon AND s.actif = true AND s.dateExpiration > :now ORDER BY s.dateCreation DESC")
    List<StorySalon> findActiveStoriesBySalonSlug(@Param("slugSalon") String slugSalon, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM StorySalon s JOIN FETCH s.salon sal WHERE s.actif = true AND s.dateExpiration > :now AND sal.statut = true ORDER BY sal.id ASC, s.dateCreation DESC")
    List<StorySalon> findAllActiveStoriesWithSalon(@Param("now") LocalDateTime now);

    List<StorySalon> findByDateExpirationBefore(LocalDateTime now);

    @Query("SELECT s FROM StorySalon s WHERE s.id = :id AND s.salon.slug = :slugSalon")
    Optional<StorySalon> findByIdAndSalonSlug(@Param("id") Long id, @Param("slugSalon") String slugSalon);

    @Query("SELECT COUNT(s) FROM StorySalon s WHERE s.salon.slug = :slugSalon AND s.actif = true AND s.dateExpiration > :now")
    long countActiveStoriesBySalonSlug(@Param("slugSalon") String slugSalon, @Param("now") LocalDateTime now);
}
