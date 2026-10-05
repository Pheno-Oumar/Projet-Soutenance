package com.kadi_aon.mon_salon.salon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.salon.entity.AffectationSalon;
import com.kadi_aon.mon_salon.salon.entity.Salon;

@Repository
public interface AffectationSalonRepository extends JpaRepository<AffectationSalon, Long> {

    @Query("SELECT a FROM AffectationSalon a JOIN FETCH a.roles WHERE a.compte.email = :email AND a.salon.slug = :slug AND a.statut = true")
    Optional<AffectationSalon> findByCompteEmailAndSalonSlugAndStatutTrue(@Param("email") String email, @Param("slug") String slug);

    @Query("SELECT a FROM AffectationSalon a JOIN FETCH a.roles WHERE a.compte.id = :compteId AND a.salon.slug = :slug AND a.statut = true")
    Optional<AffectationSalon> findByCompteIdAndSalonSlugAndStatutTrue(@Param("compteId") Long compteId, @Param("slug") String slug);

    List<AffectationSalon> findByCompteIdAndStatutTrue(Long compteId);

    List<AffectationSalon> findBySalonIdAndStatutTrue(Long salonId);

    Optional<AffectationSalon> findByCompteAndSalon(Compte compte, Salon salon);

    List<AffectationSalon> findByCompteAndSalonOrderByStatutDescIdDesc(Compte compte, Salon salon);

    @Query("SELECT DISTINCT a FROM AffectationSalon a JOIN FETCH a.roles r WHERE a.salon.id = :salonId AND r.role NOT IN (com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon.CLIENT, com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon.PROPRIETAIRE) ORDER BY a.statut DESC, a.dateDebut DESC")
    List<AffectationSalon> findEmployesBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT DISTINCT a.compte.email FROM AffectationSalon a JOIN a.roles r WHERE r.role = :role AND a.statut = true AND a.compte.statut = true")
    List<String> findDistinctEmailsByRole(@Param("role") com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon role);

    @Query("SELECT DISTINCT a.compte.email FROM AffectationSalon a JOIN a.roles r WHERE a.salon.slug = :slugSalon AND r.role = :role AND a.statut = true AND a.compte.statut = true")
    List<String> findDistinctEmailsBySalonSlugAndRole(@Param("slugSalon") String slugSalon, @Param("role") com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon role);

    @Query("SELECT COUNT(DISTINCT a.compte.id) FROM AffectationSalon a JOIN a.roles r WHERE a.salon.slug = :slugSalon AND r.role = :role AND a.statut = true")
    long countBySalonSlugAndRole(@Param("slugSalon") String slugSalon, @Param("role") com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon role);

    @Query("SELECT DISTINCT a.compte FROM AffectationSalon a JOIN a.roles r WHERE a.salon.slug = :slugSalon AND r.role = :role AND a.statut = true ORDER BY a.compte.nom ASC, a.compte.prenom ASC")
    List<Compte> findComptesBySalonSlugAndRole(@Param("slugSalon") String slugSalon, @Param("role") com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon role);

    @Query("SELECT COUNT(a) > 0 FROM AffectationSalon a JOIN a.roles r WHERE a.salon.slug = :slugSalon AND a.compte.id = :clientId AND r.role = :role AND a.statut = true")
    boolean isCompteRoleDuSalon(@Param("slugSalon") String slugSalon, @Param("clientId") Long clientId, @Param("role") com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon role);

    @Query("SELECT COUNT(DISTINCT a.compte.id) FROM AffectationSalon a JOIN a.roles r WHERE a.salon.slug = :slugSalon AND r.role <> com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon.CLIENT AND a.statut = true")
    long countEmployesBySalonSlug(@Param("slugSalon") String slugSalon);

    @Query("SELECT COUNT(a) > 0 FROM AffectationSalon a JOIN a.roles r WHERE a.compte.email = :email AND r.role = com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon.CLIENT AND a.statut = true AND a.compte.statut = true")
    boolean hasClientRole(@Param("email") String email);
}

