package com.ansar.secureapi.service;

import com.ansar.secureapi.domain.Ticket;
import com.ansar.secureapi.error.NotFoundException;
import com.ansar.secureapi.repo.TicketRepository;
import com.ansar.secureapi.repo.UserRepository;
import com.ansar.secureapi.security.AuthUser;
import com.ansar.secureapi.web.Dtos.TicketRequest;
import com.ansar.secureapi.web.Dtos.TicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final UserRepository users;

    public TicketService(TicketRepository tickets, UserRepository users) {
        this.tickets = tickets;
        this.users = users;
    }

    /**
     * IDOR guard: non-admins can only load tickets they own. A foreign ticket yields 404 (not 403)
     * so callers cannot probe which ids exist.
     */
    private Ticket load(Long id, AuthUser me) {
        return (me.isAdmin() ? tickets.findById(id) : tickets.findByIdAndOwnerId(id, me.id()))
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(AuthUser me, Pageable p) {
        return tickets.findAllByOwnerId(me.id(), p).map(TicketResponse::of);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> listAll(Pageable p) {
        return tickets.findAll(p).map(TicketResponse::of);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id, AuthUser me) {
        return TicketResponse.of(load(id, me));
    }

    @Transactional
    public TicketResponse create(TicketRequest req, AuthUser me) {
        var owner = users.getReferenceById(me.id());
        Ticket t = new Ticket(req.title(), req.description(), req.severity(), owner);
        if (req.status() != null) t.update(req.title(), req.description(), req.severity(), req.status());
        return TicketResponse.of(tickets.save(t));
    }

    @Transactional
    public TicketResponse update(Long id, TicketRequest req, AuthUser me) {
        Ticket t = load(id, me);
        t.update(req.title(), req.description(), req.severity(), req.status());
        return TicketResponse.of(t);
    }

    @Transactional
    public void delete(Long id, AuthUser me) {
        tickets.delete(load(id, me));
    }
}
