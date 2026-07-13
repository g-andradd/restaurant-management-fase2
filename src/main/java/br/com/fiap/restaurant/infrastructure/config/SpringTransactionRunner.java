package br.com.fiap.restaurant.infrastructure.config;

import br.com.fiap.restaurant.application.port.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The infrastructure half of {@link TransactionRunner}'s port contract -
 * "the infrastructure implementation is the only place that knows what
 * 'atomic' means" - backed here by a manually-built {@link TransactionTemplate}
 * around Spring Boot's autoconfigured {@link PlatformTransactionManager},
 * since Spring Boot autoconfigures the manager but not a
 * {@code TransactionTemplate} bean.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
