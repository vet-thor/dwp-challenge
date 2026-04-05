package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.EntityNotFoundException;
import uk.gov.dwp.uc.pairtest.model.Account;
import uk.gov.dwp.uc.pairtest.model.Ticket;
import uk.gov.dwp.uc.pairtest.repository.TicketRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TicketPurchaseSummaryTest {
    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketPurchaseSummary ticketPurchaseSummary;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account(1L, "John Doe", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should compute correct summary for a mixed set of ticket requests")
    void shouldReturnCorrectTotals_ForValidRequests() {
        // Arrange
        var adultRequest = new TicketTypeRequest(Type.ADULT, 2);
        var childRequest = new TicketTypeRequest(Type.CHILD, 1);

        // Given
        given(ticketRepository.findByType(TicketTypeRequest.Type.CHILD))
                .willReturn(Optional.of(new Ticket(1L, Type.CHILD, new BigDecimal("15.00"))));
        given(ticketRepository.findByType(TicketTypeRequest.Type.ADULT))
                .willReturn(Optional.of(new Ticket(2L, Type.ADULT, new BigDecimal("25.00"))));

        // When
        var summary = ticketPurchaseSummary.computeSummary(testAccount, List.of(adultRequest, childRequest));

        // Assert
        assertAll(
                () -> assertTrue(summary.hasAdult(), "Should identify that an adult ticket was requested"),
                () -> assertEquals(3, summary.totalSeatReserved(), "Should sum 2 adult + 1 child seats"),
                () -> assertEquals(new BigDecimal("65.00"), summary.totalTicketPrice(), "Should calculate  (1*15) + (2*25)"),
                () -> assertEquals(new BigDecimal("35.00"), summary.balanceAfter(), "Should subtract 65 from 100 account balance")
        );
    }

    @Test
    @DisplayName("Should mark hasAdult as false when no adult tickets are present")
    void shouldSetIsAdultFalse_WhenNoAdultIsPresent() {
        // Arrange
        var infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        var childRequest = new TicketTypeRequest(Type.CHILD, 1);

        given(ticketRepository.findByType(TicketTypeRequest.Type.INFANT))
                .willReturn(Optional.of(new Ticket(3L, TicketTypeRequest.Type.INFANT, BigDecimal.ZERO)));
        given(ticketRepository.findByType(TicketTypeRequest.Type.CHILD))
                .willReturn(Optional.of(new Ticket(2L, Type.CHILD, new BigDecimal("15.00"))));

        // Act
        var summary = ticketPurchaseSummary.computeSummary(testAccount, List.of(infantRequest, childRequest));

        // Assert
        assertFalse(summary.hasAdult(), "hasAdult should be false if no ADULT type is in the request list");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when a ticket type is missing from the repository")
    void shouldThrowException_WhenTypeIsInvalid() {
        // Arrange
        var invalidRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        given(ticketRepository.findByType(TicketTypeRequest.Type.ADULT)).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> ticketPurchaseSummary.computeSummary(testAccount, List.of(invalidRequest)),
                "Should fail if the repository doesn't recognise the ticket type"
        );
    }
}