package com.kadi_aon.mon_salon.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kadi_aon.mon_salon.account.entity.Compte;

public interface CompteRepository extends JpaRepository<Compte, Long> {

    Optional<Compte> findByEmail(String email);

    Optional<Compte> findByTelephone(String telephone);

    boolean existsByEmail(String email);

    boolean existsByTelephone(String telephone);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Compte c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR c.telephone LIKE CONCAT('%', :query, '%')")
    java.util.List<Compte> searchClients(@org.springframework.data.repository.query.Param("query") String query);

    long countByStatut(Boolean statut);
}
