package com.kadi_aon.mon_salon.favori.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.favori.entity.FavoriCoiffeur;

@Repository
public interface FavoriCoiffeurRepository extends JpaRepository<FavoriCoiffeur, Long> {

    Optional<FavoriCoiffeur> findByClientIdAndCoiffeurIdAndSalonId(Long clientId, Long coiffeurId, Long salonId);

    Optional<FavoriCoiffeur> findByClientEmailAndCoiffeurIdAndSalonSlug(String clientEmail, Long coiffeurId, String slugSalon);

    boolean existsByClientIdAndCoiffeurIdAndSalonId(Long clientId, Long coiffeurId, Long salonId);

    void deleteByClientIdAndCoiffeurIdAndSalonId(Long clientId, Long coiffeurId, Long salonId);

    List<FavoriCoiffeur> findByClientEmailAndSalonSlugOrderByDateAjoutDesc(String clientEmail, String slugSalon);

    List<FavoriCoiffeur> findByClientEmailOrderByDateAjoutDesc(String clientEmail);
}
