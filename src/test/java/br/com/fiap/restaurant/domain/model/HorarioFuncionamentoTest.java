package br.com.fiap.restaurant.domain.model;

import br.com.fiap.restaurant.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HorarioFuncionamentoTest {

    @Test
    void constructorAcceptsAberturaBeforeFechamento() {
        var horario = new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0));

        assertThat(horario.getAbertura()).isEqualTo(LocalTime.of(8, 0));
        assertThat(horario.getFechamento()).isEqualTo(LocalTime.of(22, 0));
    }

    @Test
    void constructorRejectsAberturaEqualToFechamento() {
        assertThatThrownBy(() -> new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(8, 0)))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsAberturaAfterFechamento() {
        // Accepted Phase 2 limitation: past-midnight operating hours (e.g.
        // 18:00-02:00) cannot be represented, since abertura must be strictly
        // before fechamento on the same day. See specs/modules/04-restaurant.md.
        assertThatThrownBy(() -> new HorarioFuncionamento(LocalTime.of(18, 0), LocalTime.of(2, 0)))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void equalsAndHashCodeAreBasedOnBothFields() {
        var horario1 = new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0));
        var horario2 = new HorarioFuncionamento(LocalTime.of(8, 0), LocalTime.of(22, 0));
        var horario3 = new HorarioFuncionamento(LocalTime.of(9, 0), LocalTime.of(22, 0));

        assertThat(horario1).isEqualTo(horario2).hasSameHashCodeAs(horario2);
        assertThat(horario1).isNotEqualTo(horario3);
    }
}
