package br.com.fiap.restaurant.application.port;

/**
 * Lets a use case run several repository calls as one atomic unit without
 * the application layer carrying a Spring annotation (@Transactional is
 * off-limits here by rule). The infrastructure implementation is the only
 * place that knows what "atomic" means in terms of an actual transaction
 * manager.
 */
public interface TransactionRunner {

    void run(Runnable action);
}
