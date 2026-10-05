package com.kadi_aon.mon_salon.salon.dto;

import java.util.Set;

import com.kadi_aon.mon_salon.salon.enums.TypeRoleSalon;

import jakarta.validation.constraints.NotEmpty;

public record EmployeUpdateRolesDTORequest(
        @NotEmpty(message = "Au moins un rôle salon doit être conservé")
        Set<TypeRoleSalon> roles
) {}
