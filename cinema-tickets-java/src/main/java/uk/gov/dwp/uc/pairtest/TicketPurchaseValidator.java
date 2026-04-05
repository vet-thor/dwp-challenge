package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketSummary;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

class TicketPurchaseValidator {
    private static final int MINIMUM_RESERVATION_ALLOWED = 1;
    private static final int MAXIMUM_RESERVATION_ALLOWED = 25;
    private static final int COMPARISON_LESS_THAN = 0;
    private static final long NON_ZERO_INTEGER = 1L;

    public void validatePurchaseRules(TicketSummary ticketSummary){
        if(!ticketSummary.hasAdult()){
            throw new InvalidPurchaseException("Kindly select one or more adult ticket.");
        }

        if(MINIMUM_RESERVATION_ALLOWED > ticketSummary.totalSeatReserved()){
            throw new InvalidPurchaseException("Kindly select one or more ticket.");
        }

        if(ticketSummary.totalSeatReserved() > MAXIMUM_RESERVATION_ALLOWED){
            throw new InvalidPurchaseException("You have exceeded the total ticket that can be purchased at a time.");
        }

        if(ticketSummary.balanceBefore().compareTo(ticketSummary.totalTicketPrice()) < COMPARISON_LESS_THAN){
            throw new InvalidPurchaseException(String.format("""
                    Insufficient funds: Your account balance of %.2f is less than the required total of %.2f.""",
                    ticketSummary.balanceBefore(),
                    ticketSummary.totalTicketPrice())
            );
        }
    }

    public void validateAccountId(Long accountId){
        if(NON_ZERO_INTEGER > accountId){
            throw new InvalidPurchaseException("The account you supplied is invalid.");
        }
    }
}
