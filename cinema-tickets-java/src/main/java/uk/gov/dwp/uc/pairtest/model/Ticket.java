package uk.gov.dwp.uc.pairtest.model;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import java.math.BigDecimal;

public record Ticket(Long id, TicketTypeRequest.Type type, BigDecimal price) implements IdentifiableEntity<Long> {
}
