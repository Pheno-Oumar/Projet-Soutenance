package com.kadi_aon.mon_salon.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.stock.entity.LignePanier;

@Repository
public interface LignePanierRepository extends JpaRepository<LignePanier, Long> {

    Optional<LignePanier> findByPanierIdAndProduitId(Long panierId, Long produitId);

    void deleteByPanierId(Long panierId);
}
