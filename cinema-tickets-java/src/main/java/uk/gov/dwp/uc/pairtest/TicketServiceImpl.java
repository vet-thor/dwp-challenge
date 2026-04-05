package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.EntityNotFoundException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.model.Account;
import uk.gov.dwp.uc.pairtest.repository.AccountRepository;

import java.util.List;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    private final SeatReservationService seatReservationService;
    private final TicketPaymentService ticketPaymentService;
    private final AccountRepository accountRepository;
    private final TicketPurchaseSummary ticketPurchaseSummary;
    private final TicketPurchaseValidator validator;

    public TicketServiceImpl(SeatReservationService seatReservationService, TicketPaymentService ticketPaymentService, AccountRepository accountRepository, TicketPurchaseSummary ticketPurchaseSummary, TicketPurchaseValidator validator){
        this.seatReservationService = seatReservationService;
        this.ticketPaymentService = ticketPaymentService;
        this.accountRepository = accountRepository;
        this.ticketPurchaseSummary = ticketPurchaseSummary;
        this.validator = validator;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validator.validateAccountId(accountId);

        var cutomerAccount = accountRepository.findById(accountId)
                                              .orElseThrow(
                                                      () -> new EntityNotFoundException("Customer does not exists.")
                                              );

        var ticketPurchaseSummary = this.ticketPurchaseSummary.computeSummary(
                cutomerAccount,
                List.of(ticketTypeRequests)
        );

        validator.validatePurchaseRules(ticketPurchaseSummary);

        var updatedCustomerAccount = new Account(
                cutomerAccount.id(),
                cutomerAccount.name(),
                ticketPurchaseSummary.balanceAfter()
        );


        // In a traditional relational database, we can wrap in a transaction to guarantee ACID compliance and immediate consistency
        // In a microservice with bounded context, we can leverage SAGA pattern to ensure immediate consistency
        seatReservationService.reserveSeat(accountId, ticketPurchaseSummary.totalSeatReserved());
        ticketPaymentService.makePayment(accountId, ticketPurchaseSummary.totalTicketPrice().intValue());
        accountRepository.save(updatedCustomerAccount);
    }
}
