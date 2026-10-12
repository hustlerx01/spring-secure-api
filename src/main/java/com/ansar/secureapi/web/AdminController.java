package com.ansar.secureapi.web;

import com.ansar.secureapi.repo.UserRepository;
import com.ansar.secureapi.service.TicketService;
import com.ansar.secureapi.web.Dtos.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** URL rule in SecurityConfig AND @PreAuthorize: defense in depth if a route is ever remapped. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final TicketService svc;
    private final UserRepository users;

    public AdminController(TicketService svc, UserRepository users) {
        this.svc = svc;
        this.users = users;
    }

    @GetMapping("/tickets")
    public Page<TicketResponse> allTickets(@PageableDefault(size = 20) Pageable p) { return svc.listAll(p); }

    @GetMapping("/users")
    public List<UserSummary> users() {
        return users.findAll().stream().map(u -> new UserSummary(u.getId(), u.getUsername(), u.getRole().name())).toList();
    }
}
