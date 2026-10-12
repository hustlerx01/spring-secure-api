package com.ansar.secureapi.web;

import com.ansar.secureapi.domain.Severity;
import com.ansar.secureapi.domain.Ticket;
import com.ansar.secureapi.domain.TicketStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "only letters, digits, _ . -")
            String username,
            @NotBlank @Size(min = 10, max = 72, message = "must be 10-72 characters")
            String password) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}

    public record TicketRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 2000) String description,
            @NotNull Severity severity,
            TicketStatus status) {}

    public record TicketResponse(Long id, String title, String description, Severity severity,
                                 TicketStatus status, String owner, Instant createdAt, Instant updatedAt) {
        public static TicketResponse of(Ticket t) {
            return new TicketResponse(t.getId(), t.getTitle(), t.getDescription(), t.getSeverity(),
                    t.getStatus(), t.getOwner().getUsername(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    public record UserSummary(Long id, String username, String role) {}
}
