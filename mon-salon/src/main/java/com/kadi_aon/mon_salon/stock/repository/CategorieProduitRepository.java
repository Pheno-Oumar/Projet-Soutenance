package com.kadi_aon.mon_salon.stock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.stock.entity.CategorieProduit;

@Repository
public interface CategorieProduitRepository extends JpaRepository<CategorieProduit, Long> {

    List<CategorieProduit> findBySalonSlug(String slugSalon);

    List<CategorieProduit> findBySalonSlugAndStatut(String slugSalon, boolean statut);

    Optional<CategorieProduit> findByIdAndSalonSlug(Long id, String slugSalon);

    boolean existsBySalonIdAndNomIgnoreCase(Long salonId, String nom);

    boolean existsBySalonIdAndNomIgnoreCaseAndIdNot(Long salonId, String nom, Long id);
}
