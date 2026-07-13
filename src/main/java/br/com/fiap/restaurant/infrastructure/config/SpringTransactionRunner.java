package br.com.fiap.restaurant.infrastructure.config;

import br.com.fiap.restaurant.application.port.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    // Spring Boot auto-configures a PlatformTransactionManager (JpaTransactionManager)
    // but not a TransactionTemplate bean, so it's constructed here rather than injected.
    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
