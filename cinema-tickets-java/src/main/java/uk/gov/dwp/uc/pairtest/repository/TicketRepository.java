package uk.gov.dwp.uc.pairtest.repository;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.model.Ticket;

import java.util.Optional;

public interface TicketRepository extends CrudRepository<Ticket, Long> {
    Optional<Ticket> findByType(TicketTypeRequest.Type type);
}
