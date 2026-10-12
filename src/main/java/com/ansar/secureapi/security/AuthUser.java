package com.ansar.secureapi.security;

import com.ansar.secureapi.domain.Role;

/** Principal placed in the SecurityContext after JWT validation. */
public record AuthUser(Long id, String username, Role role) {
    public boolean isAdmin() { return role == Role.ADMIN; }
}
