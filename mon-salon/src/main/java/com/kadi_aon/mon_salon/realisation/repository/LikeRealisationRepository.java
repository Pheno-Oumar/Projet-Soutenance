package com.kadi_aon.mon_salon.realisation.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.realisation.entity.LikeRealisation;
import com.kadi_aon.mon_salon.realisation.entity.Realisation;

@Repository
public interface LikeRealisationRepository extends JpaRepository<LikeRealisation, Long> {

    Optional<LikeRealisation> findByRealisationIdAndAffectationSalonId(Long realisationId, Long affectationSalonId);

    boolean existsByRealisationIdAndAffectationSalonId(Long realisationId, Long affectationSalonId);

    long countByRealisationId(Long realisationId);

    void deleteByRealisationIdAndAffectationSalonId(Long realisationId, Long affectationSalonId);

    @Query("SELECT l.realisation.id FROM LikeRealisation l WHERE l.affectationSalon.compte.email = :email")
    Set<Long> findRealisationIdsLikedByEmail(@Param("email") String email);

    @Query("SELECT l.realisation FROM LikeRealisation l WHERE l.affectationSalon.compte.email = :email ORDER BY l.dateCreation DESC")
    Page<Realisation> findRealisationLikesByEmail(@Param("email") String email, Pageable pageable);
}
