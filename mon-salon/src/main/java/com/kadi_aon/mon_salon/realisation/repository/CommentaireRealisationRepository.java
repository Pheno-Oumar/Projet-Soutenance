package com.kadi_aon.mon_salon.realisation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.realisation.entity.CommentaireRealisation;
import com.kadi_aon.mon_salon.realisation.enums.StatutCommentaire;

@Repository
public interface CommentaireRealisationRepository extends JpaRepository<CommentaireRealisation, Long> {

    @Query("SELECT c FROM CommentaireRealisation c JOIN FETCH c.affectationSalon a JOIN FETCH a.compte WHERE c.realisation.id = :realisationId AND c.statut = :statut ORDER BY c.dateCreation DESC")
    List<CommentaireRealisation> findByRealisationIdAndStatutWithCompteOrderByDateCreationDesc(
            @Param("realisationId") Long realisationId,
            @Param("statut") StatutCommentaire statut
    );

    long countByRealisationIdAndStatut(Long realisationId, StatutCommentaire statut);

    @Query("SELECT c FROM CommentaireRealisation c WHERE c.id = :id AND c.realisation.salon.slug = :slugSalon")
    Optional<CommentaireRealisation> findByIdAndRealisationSalonSlug(@Param("id") Long id, @Param("slugSalon") String slugSalon);

    @Query("SELECT c FROM CommentaireRealisation c WHERE c.id = :id AND c.affectationSalon.compte.email = :email")
    Optional<CommentaireRealisation> findByIdAndCompteEmail(@Param("id") Long id, @Param("email") String email);

    @Query("SELECT c FROM CommentaireRealisation c JOIN FETCH c.affectationSalon a JOIN FETCH a.compte JOIN FETCH c.realisation r WHERE r.salon.slug = :slugSalon ORDER BY c.dateCreation DESC")
    List<CommentaireRealisation> findByRealisationSalonSlugWithCompteAndRealisationOrderByDateCreationDesc(@Param("slugSalon") String slugSalon);
}
