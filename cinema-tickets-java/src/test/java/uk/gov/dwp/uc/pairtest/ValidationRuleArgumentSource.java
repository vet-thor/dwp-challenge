package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import java.util.Arrays;
import java.util.stream.Stream;

class ValidationRuleArgumentSource implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                createArgs(
                        100L,
                         new TicketTypeRequest[]{
                                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0)
                        },
                        "ticket purchased is less than 1",
                        "Kindly select one or more ticket."
                ),
                createArgs(
                        100L,
                         new TicketTypeRequest[]{
                                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 15),
                                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 15)
                        },
                        "ticket purchased is more than 25",
                        "You have exceeded the total ticket that can be purchased at a time."
                ),
                createArgs(
                        100L,
                        new TicketTypeRequest[]{
                                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1),
                                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1)
                        },
                        "purchased ticket without an adult ticket",
                        "Kindly select one or more adult ticket."
                ),
                createArgs(
                        100L,
                        new TicketTypeRequest[]{
                                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 10),
                                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1),
                                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 6)
                        },
                        "account holder does not have sufficient funds to pay for ticket",
                        "Insufficient funds: Your account balance of 50.00 is less than the required total of 165.00."
                ),
                createArgs(
                        0L,
                        new TicketTypeRequest[]{
                                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 10),
                                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1),
                                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 6)
                        },
                        "account ID is invalid",
                        "The account you supplied is invalid."
                )
        );
    }

    private Arguments createArgs(Long accountId, TicketTypeRequest[] requests, String ruleName, String expectedErrorMessage){
        var totalNoOfTickets = Arrays.stream(requests)
                .filter(request -> request.getTicketType() != TicketTypeRequest.Type.INFANT)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();

        return Arguments.of(accountId, requests, totalNoOfTickets, ruleName, expectedErrorMessage);
    }
}
