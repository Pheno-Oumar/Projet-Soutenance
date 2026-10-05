package com.kadi_aon.mon_salon.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.stock.entity.Panier;

@Repository
public interface PanierRepository extends JpaRepository<Panier, Long> {

    Optional<Panier> findByClientAffectationId(Long affectationId);

    Optional<Panier> findByClientAffectationCompteEmailAndClientAffectationSalonSlug(String email, String slug);

    java.util.List<Panier> findByClientAffectationCompteEmail(String email);

    boolean existsByClientAffectationId(Long affectationId);

}
