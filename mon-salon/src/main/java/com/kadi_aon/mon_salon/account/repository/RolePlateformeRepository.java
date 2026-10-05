package com.kadi_aon.mon_salon.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.account.entity.RolePlateforme;
import com.kadi_aon.mon_salon.account.enums.TypeRolePlateforme;

@Repository
public interface RolePlateformeRepository extends JpaRepository<RolePlateforme, Long> {

    Optional<RolePlateforme> findByRole(TypeRolePlateforme role);

    boolean existsByRole(TypeRolePlateforme role);
}
