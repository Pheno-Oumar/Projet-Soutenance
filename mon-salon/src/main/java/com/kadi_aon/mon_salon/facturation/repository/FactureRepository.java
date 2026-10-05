package com.kadi_aon.mon_salon.facturation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.facturation.entity.Facture;
import com.kadi_aon.mon_salon.facturation.enums.StatutFacture;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByNumeroFacture(String numeroFacture);

    Optional<Facture> findByIdAndPrestationSalonSlug(Long id, String slugSalon);

    Optional<Facture> findByPrestationId(Long prestationId);

    Optional<Facture> findByCommandeId(Long commandeId);

    List<Facture> findByPrestationSalonSlugOrderByDateEmissionDesc(String slugSalon);

    List<Facture> findByPrestationSalonSlugAndStatutOrderByDateEmissionDesc(String slugSalon, StatutFacture statut);

    List<Facture> findByPrestationClientIdOrderByDateEmissionDesc(Long clientId);

    List<Facture> findByPrestationClientIdAndPrestationSalonSlugOrderByDateEmissionDesc(Long clientId, String slugSalon);
}
