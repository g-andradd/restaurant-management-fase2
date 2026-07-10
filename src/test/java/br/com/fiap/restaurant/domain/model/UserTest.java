package br.com.fiap.restaurant.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void createGeneratesIdAndTimestamps() {
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", "Rua A, 100");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getNome()).isEqualTo("Ana Silva");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getLogin()).isEqualTo("ana.silva");
        assertThat(user.getSenhaHash()).isEqualTo("hashed-pw");
        assertThat(user.getEndereco()).isEqualTo("Rua A, 100");
        assertThat(user.getDataCriacao()).isEqualTo(user.getDataUltimaAlteracao());
    }

    @Test
    void createAllowsNullEndereco() {
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", null);

        assertThat(user.getEndereco()).isNull();
    }

    @Test
    void createRejectsBlankNome() {
        assertThatThrownBy(() -> User.create(" ", "ana@example.com", "ana.silva", "hashed-pw", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsEmailWithoutAtSign() {
        assertThatThrownBy(() -> User.create("Ana Silva", "not-an-email", "ana.silva", "hashed-pw", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsBlankLogin() {
        assertThatThrownBy(() -> User.create("Ana Silva", "ana@example.com", " ", "hashed-pw", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsBlankSenhaHash() {
        assertThatThrownBy(() -> User.create("Ana Silva", "ana@example.com", "ana.silva", " ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstituteRoundTripsExistingData() {
        UUID id = UUID.randomUUID();
        LocalDateTime criadoEm = LocalDateTime.now().minusDays(10);
        LocalDateTime alteradoEm = LocalDateTime.now().minusDays(1);

        User user = User.reconstitute(id, "Ana Silva", "ana@example.com", "ana.silva", "hashed-pw",
                "Rua A, 100", criadoEm, alteradoEm);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getDataCriacao()).isEqualTo(criadoEm);
        assertThat(user.getDataUltimaAlteracao()).isEqualTo(alteradoEm);
    }

    @Test
    void atualizarDadosChangesFieldsAndBumpsTimestamp() {
        User user = User.reconstitute(UUID.randomUUID(), "Ana Silva", "ana@example.com", "ana.silva",
                "hashed-pw", "Rua A, 100", LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(5));
        LocalDateTime alteradoAntes = user.getDataUltimaAlteracao();

        user.atualizarDados("Ana Souza", "ana.souza@example.com", "ana.souza", "Rua B, 200");

        assertThat(user.getNome()).isEqualTo("Ana Souza");
        assertThat(user.getEmail()).isEqualTo("ana.souza@example.com");
        assertThat(user.getLogin()).isEqualTo("ana.souza");
        assertThat(user.getEndereco()).isEqualTo("Rua B, 200");
        assertThat(user.getDataUltimaAlteracao()).isAfterOrEqualTo(alteradoAntes);
    }

    @Test
    void alterarSenhaChangesHashAndBumpsTimestamp() {
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null);
        LocalDateTime alteradoAntes = user.getDataUltimaAlteracao();

        user.alterarSenha("new-hash");

        assertThat(user.getSenhaHash()).isEqualTo("new-hash");
        assertThat(user.getDataUltimaAlteracao()).isAfterOrEqualTo(alteradoAntes);
    }

    @Test
    void alterarSenhaRejectsBlankHash() {
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null);

        assertThatThrownBy(() -> user.alterarSenha(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
