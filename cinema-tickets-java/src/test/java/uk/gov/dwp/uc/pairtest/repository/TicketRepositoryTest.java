package uk.gov.dwp.uc.pairtest.repository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.DuplicateEntityException;
import uk.gov.dwp.uc.pairtest.exception.RepositoryException;
import uk.gov.dwp.uc.pairtest.model.Ticket;
import uk.gov.dwp.uc.pairtest.repository.impl.TicketRepositoryImpl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TicketRepositoryTest {
    private TicketRepository ticketRepository;

    @BeforeEach
    void setup(){
        ticketRepository = new TicketRepositoryImpl();
    }

    @Nested
    class FindAllTest{
        @Test
        @DisplayName("SHOULD return an empty collection WHEN the repository is newly initialized")
        void shouldReturnEmpty_WhenRepositoryIsNew() {
            var result = ticketRepository.findAll();

            assertAll(
                    () -> assertNotNull(result, "findAll should never return null"),
                    () -> assertFalse(result.iterator().hasNext(), "New repository should be empty")
            );
        }

        @Test
        @DisplayName("Should return all persisted entities currently in storage")
        void findAll_ShouldReturnAllEntities_WhenItemsArePresent() {
            // Arrange
            var infantTicket = new Ticket(1L, Type.INFANT, new BigDecimal("0.00"));
            var adultTicket = new Ticket(2L, Type.ADULT, new BigDecimal("25.00"));
            ticketRepository.save(infantTicket);
            ticketRepository.save(adultTicket);

            // Act
            var persistedList = ticketRepository.findAll();

            // Assert
            assertAll(
                    () -> assertEquals(2, persistedList.size(), "Should find exactly two items"),
                    () -> assertTrue(persistedList.contains(infantTicket), "Should contain the first entity"),
                    () -> assertTrue(persistedList.contains(adultTicket), "Should contain the second entity")
            );
        }
    }

    @Nested
    @DisplayName("GIVEN a search by ticket ID")
    class FindByIdTest {
        @Test
        @DisplayName("SHOULD return a result WHEN the ticket exists in the repository")
        void shouldReturnTicket_WhenTicketExistById() {

            // Arrange: Save an initial ticket
            var ticketId = 1L;
            var ticket = new Ticket(ticketId, Type.ADULT, new BigDecimal("25.00"));
            ticketRepository.save(ticket);

            // Act: Search for the ticket using the id
            var persisted = ticketRepository.findById(ticketId);

            // Assert
            assertAll(
                    () -> {
                        assertTrue(persisted.isPresent(), "The entity should exist in the repository");
                        assertEquals(ticketId, persisted.get().id(), "The id should match the original");
                        assertEquals(new BigDecimal("25.00"), persisted.get().price(), "Balance should be 25.00");
                    }
            );
        }

        @Test
        @DisplayName("SHOULD return an empty result WHEN no ticket exists for the given ID")
        void shouldReturnEmpty_WhenTicketDoesNotExists(){
            var result = ticketRepository.findById(1L);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("GIVEN a search by ticket type")
    class FindByTypeTest {
        @Test@DisplayName("SHOULD return an empty result WHEN no ticket exists for given type")
        void shouldReturnEmpty_WhenTypeDoesNotExists() {
            // Act
            var result = ticketRepository.findByType(Type.INFANT);

            // Assert
            assertTrue(result.isEmpty());
        }

        @ParameterizedTest(name = "for ticket type {0}")
        @EnumSource(Type.class)
        @DisplayName("SHOULD return a result WHEN the ticket type exists in the repository")
        void shouldReturnTicket_WhenTypeExists(Type type){
            long ticketId = 1L;
            var ticket = new Ticket(ticketId, type, new BigDecimal("20.00"));
            ticketRepository.save(ticket);

            // Act:
            var persisted = ticketRepository.findByType(type);

            assertAll(
                    () -> {

                        assertTrue(persisted.isPresent(), "The entity should exist in the repository");
                        assertEquals(ticketId, persisted.get().id(), "The id should match the original");
                    }
            );
        }
    }

    @Nested
    @DisplayName("GIVEN a ticket is saved or updated")
    class SaveTest {
        @Test
        @DisplayName("SHOULD successfully persist a new ticket WHEN it does not exists in repository")
        void shouldPersistNewTicket_WhenCreated(){
            // Arrange:
            var ticketId = 1L;
            var ticket = new Ticket(ticketId, Type.ADULT, new BigDecimal("25.00"));

            // Act:
            ticketRepository.save(ticket);

            // Assert:
            assertAll(
                () -> {
                    var persisted = ticketRepository.findById(ticketId);

                    assertTrue(persisted.isPresent(), "The entity should exist in the repository");
                    assertEquals(ticketId, persisted.get().id(), "The id should match the original");
                }
            );
        }

        @Test
        @DisplayName("Should throw RepositoryException when attempting to save an entity with an ID less than 1")
        void shouldThrowRepositoryException_WhenIdIsZeroOrNegative(){
            // Arrange:
            var ticketId = 0L;
            var ticket = new Ticket(ticketId, Type.ADULT, new BigDecimal("25.00"));

            // Act:
            var exception = assertThrows(RepositoryException.class, () -> {
                ticketRepository.save(ticket);
            });

            assertEquals("ID must be greater than 0.", exception.getMessage());
        }

        @Test
        @DisplayName("SHOULD update the repository state WHEN saving a ticket with an existing ID")
        void shouldOverwrite_WhenIdAlreadyExists() {
            // Arrange: Save an initial ticket
            long ticketId = 2L;
            var initialTicket = new Ticket(ticketId, Type.ADULT, new BigDecimal("20.00"));
            ticketRepository.save(initialTicket);

            // Act:
            var modifiedTicket = new Ticket(ticketId, Type.ADULT, new BigDecimal("25.00"));
            ticketRepository.save(modifiedTicket);

            // Assert:
            var persisted = ticketRepository.findById(ticketId);

            assertTrue(persisted.isPresent());
            assertEquals(1, ticketRepository.count());
            assertEquals(ticketId, persisted.get().id());
            assertNotEquals(initialTicket, persisted.get());
            assertEquals(modifiedTicket, persisted.get());

        }

        @Test
        @DisplayName("SHOULD throw DuplicateEntityException WHEN a type is already mapped to a different entity")
        void shouldThrowException_WhenCreateDuplicateType(){

            var ticket = new Ticket(1L, Type.INFANT, new BigDecimal("0.00"));
            ticketRepository.save(ticket);

            var duplicateTypeTicket = new Ticket(2L, Type.INFANT, new BigDecimal("0.00"));

            assertAll(
                    () -> assertThrows(DuplicateEntityException.class, () -> ticketRepository.save(duplicateTypeTicket)),
                    () -> assertEquals(1, ticketRepository.count(), "Repository count should not increase")
            );
        }

        @Test
        @DisplayName("SHOULD enforce type uniqueness WHEN an existing ticket has changed its type")
        void shouldPreventDuplicateType_WhenTypeHasBeenUpdated() {
            // Arrange:
            var originalId = 1L;
            var initialTicket = new Ticket(originalId, Type.ADULT, new BigDecimal("15.00"));
            ticketRepository.save(initialTicket);

            var modifiedTicket = new Ticket(originalId, Type.INFANT, new BigDecimal("0.00"));
            ticketRepository.save(modifiedTicket);

            // Act:
            var duplicateTypeTicket = new Ticket(2L, Type.INFANT, new BigDecimal("0.00"));

            // Assert
            assertAll(
                    () -> assertThrows(DuplicateEntityException.class, () -> ticketRepository.save(duplicateTypeTicket)),
                    () -> assertEquals(1, ticketRepository.count(), "Repository count should not increase"),
                    () -> {
                        var persisted = ticketRepository.findById(originalId);
                        assertTrue(persisted.isPresent(), "The entity should exists in the repository");
                        assertEquals(Type.INFANT, persisted.get().type(), "Original ticket should remain INFANT");
                    }
            );
        }
    }

    @Nested
    @DisplayName("GIVEN all entries a deleted")
    class DeleteAllTest{
        @Test
        @DisplayName("Should clear all entries when deleteAll is called")
        void deleteAll_ShouldEmptyStorage() {
            var adultTicket = new Ticket(1L, Type.ADULT, new BigDecimal("25.00"));
            var childTicket = new Ticket(2L, Type.CHILD, new BigDecimal("15.00"));
            ticketRepository.save(adultTicket);
            ticketRepository.save(childTicket);

            ticketRepository.deleteAll();

            assertEquals(0, ticketRepository.count(), "Ensure no entity is in the repository");
            assertTrue(ticketRepository.findAll().isEmpty());
        }
    }
}