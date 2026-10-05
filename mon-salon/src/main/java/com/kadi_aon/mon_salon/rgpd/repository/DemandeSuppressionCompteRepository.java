package com.kadi_aon.mon_salon.rgpd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.rgpd.entity.DemandeSuppressionCompte;
import com.kadi_aon.mon_salon.rgpd.enums.StatutDemandeSuppression;

@Repository
public interface DemandeSuppressionCompteRepository extends JpaRepository<DemandeSuppressionCompte, Long> {

    List<DemandeSuppressionCompte> findByCompteEmailOrderByDateDemandeDesc(String email);

    List<DemandeSuppressionCompte> findByStatutOrderByDateDemandeDesc(StatutDemandeSuppression statut);

    List<DemandeSuppressionCompte> findAllByOrderByDateDemandeDesc();

    boolean existsByCompteEmailAndStatut(String email, StatutDemandeSuppression statut);
}
