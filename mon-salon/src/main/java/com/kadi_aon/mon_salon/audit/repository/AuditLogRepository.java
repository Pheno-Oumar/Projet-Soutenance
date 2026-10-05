package com.kadi_aon.mon_salon.audit.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.audit.entity.AuditLog;
import com.kadi_aon.mon_salon.audit.enums.TypeActionAudit;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByDateHeureDesc();

    List<AuditLog> findByCompteIdOrderByDateHeureDesc(Long compteId);

    List<AuditLog> findByAffectationSalonSalonIdOrderByDateHeureDesc(Long salonId);

    List<AuditLog> findByAffectationSalonSalonSlugOrderByDateHeureDesc(String slugSalon);

    Optional<AuditLog> findByIdAndAffectationSalonSalonSlug(Long id, String slugSalon);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entite IS NULL OR LOWER(a.entite) = LOWER(:entite)) AND " +
           "(:dateDebut IS NULL OR a.dateHeure >= :dateDebut) AND " +
           "(:dateFin IS NULL OR a.dateHeure <= :dateFin) " +
           "ORDER BY a.dateHeure DESC")
    List<AuditLog> filtrerLogsPlateforme(
            @Param("action") TypeActionAudit action,
            @Param("entite") String entite,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

    @Query("SELECT a FROM AuditLog a WHERE a.affectationSalon.salon.slug = :slugSalon AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entite IS NULL OR LOWER(a.entite) = LOWER(:entite)) AND " +
           "(:dateDebut IS NULL OR a.dateHeure >= :dateDebut) AND " +
           "(:dateFin IS NULL OR a.dateHeure <= :dateFin) " +
           "ORDER BY a.dateHeure DESC")
    List<AuditLog> filtrerLogsSalon(
            @Param("slugSalon") String slugSalon,
            @Param("action") TypeActionAudit action,
            @Param("entite") String entite,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

    @Query("SELECT a FROM AuditLog a WHERE " +
           "LOWER(a.entite) LIKE LOWER(CONCAT('%', :motCle, '%')) OR " +
           "LOWER(a.ancienneValeur) LIKE LOWER(CONCAT('%', :motCle, '%')) OR " +
           "LOWER(a.nouvelleValeur) LIKE LOWER(CONCAT('%', :motCle, '%')) OR " +
           "LOWER(a.roleUtilise) LIKE LOWER(CONCAT('%', :motCle, '%')) OR " +
           "LOWER(a.adresseIP) LIKE LOWER(CONCAT('%', :motCle, '%')) " +
           "ORDER BY a.dateHeure DESC")
    List<AuditLog> rechercherLogsPlateforme(@Param("motCle") String motCle);

    @Query("SELECT a FROM AuditLog a WHERE a.affectationSalon.salon.slug = :slugSalon AND (" +
           "LOWER(a.entite) LIKE LOWER(CONCAT('%', :motCle, '%')) OR " +
           "LOWER(a.ancienneValeur) LIKE LOWER(CONCAT('%', :motCle, '%')) OR " +
           "LOWER(a.nouvelleValeur) LIKE LOWER(CONCAT('%', :motCle, '%')) OR " +
           "LOWER(a.roleUtilise) LIKE LOWER(CONCAT('%', :motCle, '%')) OR " +
           "LOWER(a.adresseIP) LIKE LOWER(CONCAT('%', :motCle, '%')) ) " +
           "ORDER BY a.dateHeure DESC")
    List<AuditLog> rechercherLogsSalon(@Param("slugSalon") String slugSalon, @Param("motCle") String motCle);

    @Query("SELECT a FROM AuditLog a WHERE a.action IN :actionsSensibles OR a.entite IN :entitesSensibles ORDER BY a.dateHeure DESC")
    List<AuditLog> findActionsSensibles(
            @Param("actionsSensibles") Collection<TypeActionAudit> actionsSensibles,
            @Param("entitesSensibles") Collection<String> entitesSensibles
    );

    @Query("SELECT a FROM AuditLog a WHERE a.affectationSalon.salon.slug = :slugSalon AND (a.action IN :actionsSensibles OR a.entite IN :entitesSensibles) ORDER BY a.dateHeure DESC")
    List<AuditLog> findActionsSensiblesSalon(
            @Param("slugSalon") String slugSalon,
            @Param("actionsSensibles") Collection<TypeActionAudit> actionsSensibles,
            @Param("entitesSensibles") Collection<String> entitesSensibles
    );
}
