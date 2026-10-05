package com.kadi_aon.mon_salon.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.stock.entity.StockProduit;

@Repository
public interface StockProduitRepository extends JpaRepository<StockProduit, Long> {

    Optional<StockProduit> findByProduitId(Long produitId);
}
