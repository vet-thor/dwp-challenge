package uk.gov.dwp.uc.pairtest.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.exception.RepositoryException;
import uk.gov.dwp.uc.pairtest.model.Account;
import uk.gov.dwp.uc.pairtest.repository.impl.AccountRepositoryImpl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountRepositoryTest {
    private AccountRepository accountRepository;

    private static final BigDecimal ACCOUNT_BALANCE = new BigDecimal("240.00");

    @BeforeEach
    void setup(){
        accountRepository = new AccountRepositoryImpl();
    }

    @Nested
    @DisplayName("GIVEN retrieve all entities")
    class FindAllTest{
        @Test
        @DisplayName("SHOULD return an empty collection WHEN the repository is newly initialized")
        void shouldReturnEmpty_WhenRepositoryIsNew() {
            var result = accountRepository.findAll();

            assertAll(
                    () -> assertNotNull(result, "findAll should never return null"),
                    () -> assertFalse(result.iterator().hasNext(), "New repository should be empty")
            );
        }

        @Test
        @DisplayName("Should return all persisted entities currently in storage")
        void findAll_ShouldReturnAllEntities_WhenItemsArePresent() {
            // Arrange
            var customerAccountForJohn = new Account(1L, "John Doe", ACCOUNT_BALANCE);
            var customerAccountForJames = new Account(2L, "James Doe", ACCOUNT_BALANCE);
            accountRepository.save(customerAccountForJohn);
            accountRepository.save(customerAccountForJames);

            // Act
            var persistedList = accountRepository.findAll();

            // Assert
            assertAll(
                    () -> assertEquals(2, persistedList.size(), "Should find exactly two items"),
                    () -> assertTrue(persistedList.contains(customerAccountForJohn), "Should contain the first entity"),
                    () -> assertTrue(persistedList.contains(customerAccountForJames), "Should contain the second entity")
            );
        }
    }

    @Nested
    @DisplayName("GIVEN a search by account ID")
    class FindByIdTest {
        @Test
        @DisplayName("SHOULD return a result WHEN the account exists in the repository")
        void shouldReturnAccount_WhenAccountExistById() {

            // Arrange:
            var accountId = 1L;
            var account = new Account(1L, "John Doe", ACCOUNT_BALANCE);
            accountRepository.save(account);

            // Act:
            var persisted = accountRepository.findById(accountId);

            // Assert
            assertAll(
                    () -> {
                        assertTrue(persisted.isPresent(), "The entity should exist in the repository");
                        assertEquals(accountId, persisted.get().id(), "The id should match the original");
                        assertEquals("John Doe", persisted.get().name(), "Balance should be 20.00");
                    }
            );
        }

        @Test
        @DisplayName("SHOULD return an empty result WHEN no account exists for the given ID")
        void shouldReturnEmpty_WhenAccountDoesNotExists(){
            var result = accountRepository.findById(1L);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("GIVEN an account is saved or updated")
    class SaveTest {
        @Test
        @DisplayName("SHOULD successfully persist a new account WHEN it does not exists in repository")
        void shouldPersistNewAccount_WhenCreated(){
            // Arrange:
            var accountId = 1L;
            var customerAccount = new Account(1L, "John Doe", ACCOUNT_BALANCE);

            // Act:
            accountRepository.save(customerAccount);

            // Assert:
            assertAll(
                    () -> {
                        var persisted = accountRepository.findById(accountId);

                        assertTrue(persisted.isPresent(), "The entity should exist in the repository");
                        assertEquals(accountId, persisted.get().id(), "The id should match the original");
                    }
            );
        }

        @Test
        @DisplayName("Should throw RepositoryException when attempting to save an entity with an ID less than 1")
        void shouldThrowRepositoryException_WhenIdIsZeroOrNegative(){
            // Arrange:
            var ticket = new Account(0L, "John Doe", new BigDecimal("25.00"));

            // Act:
            var exception = assertThrows(RepositoryException.class, () -> {
                accountRepository.save(ticket);
            });

            assertEquals("ID must be greater than 0.", exception.getMessage());
        }

        @Test
        @DisplayName("SHOULD update the repository state WHEN saving a account with an existing ID")
        void shouldOverwrite_WhenIdAlreadyExists() {
            // Arrange:
            long accountId = 2L;
            var newBalance = new BigDecimal("100.00");
            var initialCustomerAccount = new Account(accountId, "John Doe", ACCOUNT_BALANCE);
            accountRepository.save(initialCustomerAccount);

            // Act:
            var modifiedCustomerAccount = new Account(accountId, "John Doe", newBalance);
            accountRepository.save(modifiedCustomerAccount);

            // Assert:
            var persisted = accountRepository.findById(accountId);

            assertTrue(persisted.isPresent());
            assertEquals(1, accountRepository.count(), "Repository count should not increase");
            assertEquals(accountId, persisted.get().id(), "The id should match the original");
            assertEquals(modifiedCustomerAccount, persisted.get(), "The modified entity should match the findById result");
            assertEquals(newBalance, persisted.get().balance(), "The balance should update");

        }
    }

    @Nested
    @DisplayName("GIVEN all entries a deleted")
    class DeleteAllTest{
        @Test
        @DisplayName("SHOULD clear all entries WHEN deleteAll is called")
        void deleteAll_ShouldEmptyStorage() {
            var customerAccountJohn = new Account(100L, "John Doe", ACCOUNT_BALANCE);
            var customerAccountJoe = new Account(101L, "Joe Doe", ACCOUNT_BALANCE);
            accountRepository.save(customerAccountJohn);
            accountRepository.save(customerAccountJoe);

            accountRepository.deleteAll();

            assertEquals(0, accountRepository.count(), "Ensure no entity is in the repository");
            assertTrue(accountRepository.findAll().isEmpty());
        }
    }
}