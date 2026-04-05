package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.EntityNotFoundException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.model.Account;
import uk.gov.dwp.uc.pairtest.model.Ticket;
import uk.gov.dwp.uc.pairtest.repository.AccountRepository;
import uk.gov.dwp.uc.pairtest.repository.TicketRepository;
import uk.gov.dwp.uc.pairtest.repository.impl.AccountRepositoryImpl;
import uk.gov.dwp.uc.pairtest.repository.impl.TicketRepositoryImpl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketServiceIntegrationTest {
    private TicketPaymentService ticketPaymentService;
    private SeatReservationService seatReservationService;
    private AccountRepository accountRepository;
    private TicketRepository ticketRepository;
    private TicketPurchaseValidator validator;
    private TicketPurchaseSummary ticketPurchaseSummary;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setup(){
        ticketPaymentService = mock(TicketPaymentService.class);
        seatReservationService = mock(SeatReservationService.class);
        accountRepository = new AccountRepositoryImpl();
        ticketRepository = new TicketRepositoryImpl();
        validator = new TicketPurchaseValidator();
        ticketPurchaseSummary = new TicketPurchaseSummary(ticketRepository);

        ticketService = new TicketServiceImpl(
                seatReservationService,
                ticketPaymentService,
                accountRepository,
                ticketPurchaseSummary,
                validator
        );

        setupTestData();
    }

    @Test
    @DisplayName("Should update account balance and call external services when a valid purchase is made")
    void shouldExecuteFullBusinessFlow() {
        // Arrange
        var accountId = 100L;
        var infantTicketRequest = new TicketTypeRequest(Type.INFANT, 2);
        var adultTicketRequest = new TicketTypeRequest(Type.ADULT, 1);

        // Act
        ticketService.purchaseTickets(accountId, infantTicketRequest, adultTicketRequest);

        // Verify
        assertAll(
                () -> {
                    var updatedAccount = accountRepository.findById(accountId);
                    assertTrue(updatedAccount.isPresent(), "Should find the entity");
                    assertEquals(new BigDecimal("25.00"), updatedAccount.get().balance(), "Balance should be 50 - 25");
                },
                () -> verify(
                            seatReservationService,
                            times(1)
                                    .description("Should reserve exactly one seat for adult. Infant are not allocated a seat")
                    ).reserveSeat(accountId, 1),
                () -> verify(
                        ticketPaymentService,
                        times(1)
                                .description("Total price should be 25.00. Infants are free")
                ).makePayment(accountId, 25)
        );

    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when entity is not present in the repository")
    void shouldFail_WhenAccountDoesNotExists(){
        // Arrange
        var accountId = 101L;
        var request = new TicketTypeRequest(Type.ADULT, 1);

        // Act
        var exception = assertThrows(EntityNotFoundException.class, () -> {
            ticketService.purchaseTickets(accountId, request);
        });

        // Verify
        assertAll(
                () -> assertEquals("Customer does not exists.", exception.getMessage(), "Should fail with the following message"),
                () -> verifyNoInteractions(seatReservationService, ticketPaymentService)
        );

    }

    @ParameterizedTest(name = "When {3}")
    @ArgumentsSource(ValidationRuleArgumentSource.class)
    @DisplayName("Should throw InvalidPurchaseException for a violated validation rule")
    void shouldFail_WhenValidatorDetectsException(Long accountId, TicketTypeRequest[] requests, int totalNoOfTicket, String ruleName, String expectedErrorMessage){

        // Act
        var exception = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(accountId, requests));

        // Verify
        assertAll(
                () -> assertEquals(expectedErrorMessage, exception.getMessage()),
                () -> verifyNoInteractions(seatReservationService, ticketPaymentService)
        );
    }

    @Test
    @DisplayName("Should throw EntityNotFound when input parameter type is null")
    void shouldFail_WhenTicketTypeIsNull(){
        // Arrange
        var accountId = 100L;
        var request = new TicketTypeRequest(null, 1);

        // Act
        var exception = assertThrows(EntityNotFoundException.class,
                () -> ticketService.purchaseTickets(accountId, request));

        // Verify
        assertAll(
                () -> assertEquals("Please select a valid ticket type.", exception.getMessage()),
                () -> verifyNoInteractions(seatReservationService, ticketPaymentService)
        );
    }

    private void setupTestData() {
        ticketRepository.save(new Ticket(1L, Type.ADULT, new BigDecimal("25.00")));
        ticketRepository.save(new Ticket(2L, Type.CHILD, new BigDecimal("15.00")));
        ticketRepository.save(new Ticket(3L, Type.INFANT, BigDecimal.ZERO));

        accountRepository.save(new Account(100L, "Integration User", new BigDecimal("50.00")));
    }
}
