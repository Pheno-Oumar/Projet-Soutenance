package com.kadi_aon.mon_salon.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.stock.entity.MouvementStock;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByProduitCategorieSalonSlugOrderByDateMouvementDesc(String slugSalon);

    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(Long produitId);

    List<MouvementStock> findByProduitIdAndProduitCategorieSalonSlugOrderByDateMouvementDesc(Long produitId, String slugSalon);
}
