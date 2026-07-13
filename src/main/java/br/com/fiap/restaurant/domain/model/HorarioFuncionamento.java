package br.com.fiap.restaurant.domain.model;

import br.com.fiap.restaurant.domain.exception.DomainValidationException;

import java.time.LocalTime;
import java.util.Objects;

/**
 * A restaurant's opening/closing time window. The constructor enforces
 * {@code abertura} strictly before {@code fechamento} - an accepted
 * limitation of this rule is that overnight/past-midnight hours (e.g.
 * open 22:00, close 02:00) cannot be represented at all, not just
 * disallowed by convention (see {@code specs/modules/04-restaurant.md}).
 */
public final class HorarioFuncionamento {

    private final LocalTime abertura;
    private final LocalTime fechamento;

    public HorarioFuncionamento(LocalTime abertura, LocalTime fechamento) {
        this.abertura = Objects.requireNonNull(abertura, "abertura must not be null");
        this.fechamento = Objects.requireNonNull(fechamento, "fechamento must not be null");
        if (!abertura.isBefore(fechamento)) {
            throw new DomainValidationException("abertura must be before fechamento");
        }
    }

    public LocalTime getAbertura() {
        return abertura;
    }

    public LocalTime getFechamento() {
        return fechamento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HorarioFuncionamento other)) {
            return false;
        }
        return abertura.equals(other.abertura) && fechamento.equals(other.fechamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(abertura, fechamento);
    }
}
