package edu.itba.fieldops.frameworks;

import org.springframework.transaction.support.TransactionOperations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

final class TransactionalUseCase {
    private TransactionalUseCase() {
    }

    static <T> T around(Class<T> port, T useCase, TransactionOperations transactions) {
        Objects.requireNonNull(useCase, "use case");
        Objects.requireNonNull(transactions, "transactions");
        return port.cast(Proxy.newProxyInstance(
                port.getClassLoader(),
                new Class<?>[]{port},
                (proxy, method, arguments) -> transactions.execute(status -> invoke(method, useCase, arguments))
        ));
    }

    private static Object invoke(Method method, Object useCase, Object[] arguments) {
        try {
            return method.invoke(useCase, arguments);
        } catch (InvocationTargetException failure) {
            throw unchecked(failure.getCause());
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static RuntimeException unchecked(Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(cause);
    }
}
