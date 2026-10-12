package com.ansar.secureapi.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    protected Ticket() {}

    public Ticket(String title, String description, Severity severity, AppUser owner) {
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public TicketStatus getStatus() { return status; }
    public AppUser getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String title, String description, Severity severity, TicketStatus status) {
        this.title = title;
        this.description = description;
        this.severity = severity;
        if (status != null) this.status = status;
        this.updatedAt = Instant.now();
    }
}
