package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketSummary;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.EntityNotFoundException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.model.Account;
import uk.gov.dwp.uc.pairtest.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock
    private TicketPaymentService ticketPaymentService;
    @Mock
    private SeatReservationService seatReservationService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TicketPurchaseValidator validator;
    @Mock
    private TicketPurchaseSummary ticketPurchaseSummary;
    @InjectMocks
    private TicketServiceImpl ticketService;

    private static final long VALID_ACCOUNT_ID = 1234;

    private Account customerAccount;
    private TicketSummary ticketSummary;

    @BeforeEach
    void setup(){
        customerAccount = new Account(
                VALID_ACCOUNT_ID, // Customer Account ID
                "John Doe", // Customer Name
                new BigDecimal(240) // Customer Balance
        );
        ticketSummary = new TicketSummary(
                true, // has Adult
                4, // Total Seat Reserved
                new BigDecimal("100"), // Total Ticket Price
                new BigDecimal("240"), // Balance After
                new BigDecimal("140") // Balance After
        );
    }

    @Test
    @DisplayName("SHOULD stop execution WHEN business rule validation fails")
    void shouldThrowExceptionIfTicketIsPurchasedWithoutAnAdult(){
        var infantTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        var childTicketRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

        // Given
        given(accountRepository.findById(VALID_ACCOUNT_ID)).willReturn(Optional.of(customerAccount));
        given(ticketPurchaseSummary.computeSummary(eq(customerAccount), anyList())).willReturn(ticketSummary);

        doThrow(InvalidPurchaseException.class).when(validator).validatePurchaseRules(ticketSummary);

        // When\Then
        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, infantTicketRequest, childTicketRequest);
        });

        // Verify
        assertAll(
                () -> verify(accountRepository).findById(VALID_ACCOUNT_ID),
                () -> verifyNoInteractions(seatReservationService, ticketPaymentService),
                () -> verify(accountRepository, never()).save(any())
        );
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when account does not exist")
    void purchaseTickets_ShouldThrowException_WhenAccountNotFound() {
        var ticketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        // Given
        given(accountRepository.findById(VALID_ACCOUNT_ID)).willReturn(Optional.empty());

        // When\Then
        assertThrows(EntityNotFoundException.class, () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequest));

        // Verify
        assertAll(
                () -> verifyNoInteractions(seatReservationService, ticketPaymentService),
                () -> verify(accountRepository, never()).save(any())
        );
    }

    @Test
    @DisplayName("SHOULD successfully complete all purchase WHEN all validations pass")
    void shouldPurchaseTicketSuccessfully(){
        var infantTicketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 4);
        var adultTicketTypeRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4);

        // Given
        given(accountRepository.findById(VALID_ACCOUNT_ID)).willReturn(Optional.of(customerAccount));
        given(ticketPurchaseSummary.computeSummary(eq(customerAccount), anyList())).willReturn(ticketSummary);

        // When\Then
        assertDoesNotThrow(() -> {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, infantTicketTypeRequest, adultTicketTypeRequest);
        });

        // Verify
        assertAll(
                () -> verify(validator).validateAccountId(VALID_ACCOUNT_ID),
                () -> verify(validator).validatePurchaseRules(ticketSummary),
                () -> verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, ticketSummary.totalSeatReserved()),
                () -> verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, ticketSummary.totalTicketPrice().intValue()),
                () -> verify(accountRepository).save(
                        argThat(account -> Objects.equals(account.balance(), ticketSummary.balanceAfter()))
                ),
                () -> verify(accountRepository, times(1)).findById(VALID_ACCOUNT_ID)
        );
    }
}