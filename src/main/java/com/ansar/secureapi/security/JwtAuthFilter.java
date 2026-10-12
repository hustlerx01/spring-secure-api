package com.ansar.secureapi.security;

import com.ansar.secureapi.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwt.parse(header.substring(7)).ifPresent(claims ->
                // Re-load the user so a deleted account's token stops working and the role comes from the DB,
                // not from a client-held claim.
                users.findByUsername(claims.getSubject()).ifPresent(u -> {
                    var auth = new UsernamePasswordAuthenticationToken(
                            new AuthUser(u.getId(), u.getUsername(), u.getRole()), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }));
        }
        chain.doFilter(req, res);
    }
}
