package com.kadi_aon.mon_salon.caisse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.caisse.entity.SessionCaisse;
import com.kadi_aon.mon_salon.caisse.enums.StatutSessionCaisse;

@Repository
public interface SessionCaisseRepository extends JpaRepository<SessionCaisse, Long> {

    Optional<SessionCaisse> findByAffectationSalonSlugAndStatut(String slugSalon, StatutSessionCaisse statut);

    boolean existsByAffectationSalonSlugAndStatut(String slugSalon, StatutSessionCaisse statut);

    List<SessionCaisse> findByAffectationSalonSlugOrderByDateOuvertureDesc(String slugSalon);
}
