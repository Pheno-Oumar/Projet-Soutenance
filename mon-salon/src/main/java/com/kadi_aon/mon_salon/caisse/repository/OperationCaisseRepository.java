package com.kadi_aon.mon_salon.caisse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.caisse.entity.OperationCaisse;

@Repository
public interface OperationCaisseRepository extends JpaRepository<OperationCaisse, Long> {

    List<OperationCaisse> findBySessionCaisseIdOrderByDateOperationDesc(Long sessionCaisseId);

    List<OperationCaisse> findBySessionCaisseAffectationSalonSlugOrderByDateOperationDesc(String slugSalon);
}
