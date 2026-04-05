# Ticket Booking Service
This is a small Java based system for handling ticket sales. It manages the logic for calculating prices, checking if a customer has enough money, and making sure all the booking rules are followed before a seat is reserved.

## The Rules (What this does)
We have built the service to follow these specific requirements:
- **Maximum 25 Tickets:** You cannot buy more than 25 tickets in one go. If you try, the system will block it.
- **Infants go Free:** We do not charge for infant tickets.
- **No Seats for Infants:** Infants are expected to sit on an adult's lap, so the system does not reserve a seat for them.
- **Adult Required:** You cannot buy a Child or Infant ticket on its own. There must be at least one Adult ticket in the basket.
- **Valid Account IDs:** An account ID must be a positive number (1 or higher). Anything else is rejected.
- **Sufficient Funds:** The system checks the account balance. If there is not enough money to cover the total cost, the purchase would not go through.
## Architecture Overview
The project is split into a few simple parts to keep the code tidy:
- **Service Layer:** Coordinates payments, seat reservations, and account updates.
- **Validator:** Dedicated engine that checks all the rules mentioned above.
- **Summary Tool:** A helper that handles maths like calculating total price, seat count, account balance.
- **Repository:** A simple in-memory store that holds our account and ticket data.

## How to Test
Everything has been thoroughly tested using JUnit 5 and Mockito. You can run the tests to see the rules in action.
### Running the Tests
If you have Maven installed, just run this in your terminal:
```bash
mvn test
```
