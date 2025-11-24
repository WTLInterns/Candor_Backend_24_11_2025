package com.fieldforcepro.dto.auth;

import com.fieldforcepro.model.UserRole;

public record UserDto(
        String id,
        String email,
        String name,
        UserRole role,
        boolean isActive
) {
}
