package uk.gov.dwp.uc.pairtest.domain;

import java.math.BigDecimal;

public record TicketSummary(
        boolean hasAdult,
        int totalSeatReserved,
        BigDecimal totalTicketPrice,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter
) { }
