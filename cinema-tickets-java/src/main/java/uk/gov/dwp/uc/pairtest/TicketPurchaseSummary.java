package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketSummary;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.EntityNotFoundException;
import uk.gov.dwp.uc.pairtest.model.Account;
import uk.gov.dwp.uc.pairtest.repository.TicketRepository;

import java.math.BigDecimal;
import java.util.List;

class TicketPurchaseSummary {
    private final TicketRepository ticketRepository;

    public TicketPurchaseSummary(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public TicketSummary computeSummary(Account account, List<TicketTypeRequest> ticketTypeRequests){

        var isAdult = false;
        var totalSeatReservation = 0;
        var totalTicketPrice = BigDecimal.ZERO;

        for(var request : ticketTypeRequests){
            var ticketPrice = computeTicketPrice(request);

            if(!isAdult && request.getTicketType() == TicketTypeRequest.Type.ADULT){
                isAdult = true;
            }

            if(request.getTicketType() != TicketTypeRequest.Type.INFANT) {
                totalSeatReservation += request.getNoOfTickets();
            }

            totalTicketPrice = totalTicketPrice.add(ticketPrice);
        }

        var balanceAfter = computeBalanceAfter(account, totalTicketPrice);

        return new TicketSummary(
                isAdult,
                totalSeatReservation,
                totalTicketPrice,
                account.balance(),
                balanceAfter
        );
    }

    private BigDecimal computeTicketPrice(TicketTypeRequest ticketTypeRequest){
        // This would not scale with a traditional database due to I/O overhead, but it is the most efficient approach
        // for our current in memory storage.
        var price = ticketRepository.findByType(ticketTypeRequest.getTicketType())
                .orElseThrow(() -> new EntityNotFoundException("Please select a valid ticket type."))
                .price();

        return price.multiply(new BigDecimal(ticketTypeRequest.getNoOfTickets()));
    }

    private BigDecimal computeBalanceAfter(Account account, BigDecimal totalTicketPrice){

        return account.balance().subtract(totalTicketPrice);
    }
}