package com.ansar.secureapi.web;

import com.ansar.secureapi.security.AuthUser;
import com.ansar.secureapi.service.TicketService;
import com.ansar.secureapi.web.Dtos.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {
    private final TicketService svc;

    public TicketController(TicketService svc) { this.svc = svc; }

    @GetMapping
    public Page<TicketResponse> list(@AuthenticationPrincipal AuthUser me, @PageableDefault(size = 20) Pageable p) {
        return svc.list(me, p);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return svc.get(id, me); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody TicketRequest req, @AuthenticationPrincipal AuthUser me) {
        return svc.create(req, me);
    }

    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody TicketRequest req,
                                 @AuthenticationPrincipal AuthUser me) {
        return svc.update(id, req, me);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { svc.delete(id, me); }
}
