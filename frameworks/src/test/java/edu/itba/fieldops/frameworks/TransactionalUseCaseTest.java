package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.shared.UnknownResource;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionalUseCaseTest {
    private final CountingTransactions transactions = new CountingTransactions();

    @Test
    void eachCallRunsInItsOwnTransactionAndReturnsWhatTheUseCaseReturns() {
        Greeting greeting = TransactionalUseCase.around(Greeting.class, name -> "hello " + name, transactions);

        String first = greeting.greet("Ada");
        String second = greeting.greet("Bob");

        assertAll(
                () -> assertEquals("hello Ada", first),
                () -> assertEquals("hello Bob", second),
                () -> assertEquals(2, transactions.opened)
        );
    }

    @Test
    void aFailureOfTheUseCaseLeavesTheTransactionUnwrapped() {
        Greeting greeting = TransactionalUseCase.around(Greeting.class, name -> {
            throw new UnknownResource("person", name);
        }, transactions);

        assertThrows(UnknownResource.class, () -> greeting.greet("Ghost"));
    }

    interface Greeting {
        String greet(String name);
    }

    private static final class CountingTransactions implements TransactionOperations {
        private int opened;

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            opened++;
            return action.doInTransaction(new SimpleTransactionStatus());
        }
    }
}
