package com.kadi_aon.mon_salon.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.account.entity.Compte;
import com.kadi_aon.mon_salon.account.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByCompteAndRevokedFalse(Compte compte);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.compte = :compte AND r.revoked = false")
    int revokeAllByCompte(@Param("compte") Compte compte);

    void deleteByCompte(Compte compte);
}
