package com.kadi_aon.mon_salon.favori.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.favori.entity.FavoriSalon;

@Repository
public interface FavoriSalonRepository extends JpaRepository<FavoriSalon, Long> {

    Optional<FavoriSalon> findByClientIdAndSalonId(Long clientId, Long salonId);

    Optional<FavoriSalon> findByClientEmailAndSalonSlug(String clientEmail, String slugSalon);

    boolean existsByClientIdAndSalonId(Long clientId, Long salonId);

    void deleteByClientIdAndSalonId(Long clientId, Long salonId);

    List<FavoriSalon> findByClientEmailOrderByDateAjoutDesc(String clientEmail);
}
