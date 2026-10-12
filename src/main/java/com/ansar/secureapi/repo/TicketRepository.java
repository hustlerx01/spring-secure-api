package com.ansar.secureapi.repo;

import com.ansar.secureapi.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    /** Ownership is enforced in the query itself, so a foreign id simply returns empty (IDOR guard). */
    Optional<Ticket> findByIdAndOwnerId(Long id, Long ownerId);
    Page<Ticket> findAllByOwnerId(Long ownerId, Pageable pageable);
}
