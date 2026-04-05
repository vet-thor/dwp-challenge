package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.domain.TicketSummary;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TicketPurchaseValidatorTest {
    private TicketPurchaseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TicketPurchaseValidator();
    }

    @Test
    @DisplayName("Should throw exception when no adult ticket is present")
    void shouldThrowException_WhenNoAdultTicketPresent() {
        var summary = new TicketSummary(
                false,
                1,
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                new BigDecimal("90.00")
        );

        var exception = assertThrows(InvalidPurchaseException.class,
                () -> validator.validatePurchaseRules(summary));

        assertEquals("Kindly select one or more adult ticket.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when total seats are below minimum")
    void shouldThrowException_WhenSeatsAreBelowMinimum() {
        var summary = new TicketSummary(
                true,
                0,
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                new BigDecimal("80.00")
        );

        var exception = assertThrows(InvalidPurchaseException.class,
                () -> validator.validatePurchaseRules(summary));

        assertEquals("Kindly select one or more ticket.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when total seats exceed maximum limit")
    void shouldThrowException_WhenSeatsExceedMaximum() {
        var summary = new TicketSummary(
                true,
                26,
                new BigDecimal("520.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("480.00")
        );

        var exception = assertThrows(InvalidPurchaseException.class,
                () -> validator.validatePurchaseRules(summary));

        assertEquals("You have exceeded the total ticket that can be purchased at a time.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when balance after purchase is less than total price")
    void shouldThrowException_WhenBalanceIsInsufficient() {
        var summary = new TicketSummary(
                true,
                1,
                new BigDecimal("100.00"),
                new BigDecimal("80.00"),
                new BigDecimal("-20.00")
        );

        var exception = assertThrows(InvalidPurchaseException.class,
                () -> validator.validatePurchaseRules(summary));

        assertTrue(exception.getMessage().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("Should pass when balance and rules are valid")
    void shouldPass_WhenAllConditionsAreMet() {
        var summary = new TicketSummary(
                true,
                5,
                new BigDecimal("100.00"),
                new BigDecimal("250.00"),
                new BigDecimal("150.00")
        );

        assertDoesNotThrow(() -> validator.validatePurchaseRules(summary));
    }

    @Test
    @DisplayName("Should throw exception for invalid account IDs")
    void shouldThrowException_WhenIdIsInvalid() {
        assertAll(
                () -> assertThrows(InvalidPurchaseException.class, () -> validator.validateAccountId(0L), "Should be invalid when ID is less than 1"),
                () -> assertThrows(InvalidPurchaseException.class, () -> validator.validateAccountId(-5L), "Should be invalid when ID is less than 1")
        );
    }

    @Test
    @DisplayName("Should pass for valid account IDs")
    void shouldPass_WhenIdIsValid() {
        assertDoesNotThrow(() -> validator.validateAccountId(1L), "Should be valid when ID is greater or equals to 1");
        assertDoesNotThrow(() -> validator.validateAccountId(999L), "Should be valid when ID is greater or equals to 1");
    }

}