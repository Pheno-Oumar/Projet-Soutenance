package com.kadi_aon.mon_salon.salon.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kadi_aon.mon_salon.salon.entity.RoleSalon;
import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;

@Repository
public interface RoleSalonRepository extends JpaRepository<RoleSalon, Long> {

    Optional<RoleSalon> findByRole(TypeRoleSalon role);

    boolean existsByRole(TypeRoleSalon role);
}
