package br.com.fiap.restaurant.domain.model;

import java.time.LocalTime;
import java.util.Objects;

public final class HorarioFuncionamento {

    private final LocalTime abertura;
    private final LocalTime fechamento;

    public HorarioFuncionamento(LocalTime abertura, LocalTime fechamento) {
        this.abertura = Objects.requireNonNull(abertura, "abertura must not be null");
        this.fechamento = Objects.requireNonNull(fechamento, "fechamento must not be null");
        if (!abertura.isBefore(fechamento)) {
            throw new IllegalArgumentException("abertura must be before fechamento");
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
