package br.com.fiap.restaurant.domain.model;

import br.com.fiap.restaurant.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final UUID USER_TYPE_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_TYPE_ID = UUID.randomUUID();

    @Test
    void createGeneratesIdAndTimestamps() {
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", "Rua A, 100", USER_TYPE_ID);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getNome()).isEqualTo("Ana Silva");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getLogin()).isEqualTo("ana.silva");
        assertThat(user.getSenhaHash()).isEqualTo("hashed-pw");
        assertThat(user.getEndereco()).isEqualTo("Rua A, 100");
        assertThat(user.getUserTypeId()).isEqualTo(USER_TYPE_ID);
        assertThat(user.getDataCriacao()).isEqualTo(user.getDataUltimaAlteracao());
    }

    @Test
    void createAllowsNullEndereco() {
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", null, USER_TYPE_ID);

        assertThat(user.getEndereco()).isNull();
    }

    @Test
    void createRejectsBlankNome() {
        assertThatThrownBy(() -> User.create(" ", "ana@example.com", "ana.silva", "hashed-pw", null, USER_TYPE_ID))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void createRejectsEmailWithoutAtSign() {
        assertThatThrownBy(() -> User.create("Ana Silva", "not-an-email", "ana.silva", "hashed-pw", null, USER_TYPE_ID))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void createRejectsBlankLogin() {
        assertThatThrownBy(() -> User.create("Ana Silva", "ana@example.com", " ", "hashed-pw", null, USER_TYPE_ID))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void createRejectsBlankSenhaHash() {
        assertThatThrownBy(() -> User.create("Ana Silva", "ana@example.com", "ana.silva", " ", null, USER_TYPE_ID))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void createRejectsNullUserTypeId() {
        assertThatThrownBy(() -> User.create("Ana Silva", "ana@example.com", "ana.silva", "hashed-pw", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reconstituteRoundTripsExistingData() {
        UUID id = UUID.randomUUID();
        LocalDateTime criadoEm = LocalDateTime.now().minusDays(10);
        LocalDateTime alteradoEm = LocalDateTime.now().minusDays(1);

        User user = User.reconstitute(id, "Ana Silva", "ana@example.com", "ana.silva", "hashed-pw",
                "Rua A, 100", USER_TYPE_ID, criadoEm, alteradoEm);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUserTypeId()).isEqualTo(USER_TYPE_ID);
        assertThat(user.getDataCriacao()).isEqualTo(criadoEm);
        assertThat(user.getDataUltimaAlteracao()).isEqualTo(alteradoEm);
    }

    @Test
    void atualizarDadosChangesFieldsAndBumpsTimestamp() {
        User user = User.reconstitute(UUID.randomUUID(), "Ana Silva", "ana@example.com", "ana.silva",
                "hashed-pw", "Rua A, 100", USER_TYPE_ID, LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(5));
        LocalDateTime alteradoAntes = user.getDataUltimaAlteracao();

        user.atualizarDados("Ana Souza", "ana.souza@example.com", "ana.souza", "Rua B, 200", OTHER_USER_TYPE_ID);

        assertThat(user.getNome()).isEqualTo("Ana Souza");
        assertThat(user.getEmail()).isEqualTo("ana.souza@example.com");
        assertThat(user.getLogin()).isEqualTo("ana.souza");
        assertThat(user.getEndereco()).isEqualTo("Rua B, 200");
        assertThat(user.getUserTypeId()).isEqualTo(OTHER_USER_TYPE_ID);
        assertThat(user.getDataUltimaAlteracao()).isAfterOrEqualTo(alteradoAntes);
    }

    @Test
    void alterarSenhaChangesHashAndBumpsTimestamp() {
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null, USER_TYPE_ID);
        LocalDateTime alteradoAntes = user.getDataUltimaAlteracao();

        user.alterarSenha("new-hash");

        assertThat(user.getSenhaHash()).isEqualTo("new-hash");
        assertThat(user.getDataUltimaAlteracao()).isAfterOrEqualTo(alteradoAntes);
    }

    @Test
    void alterarSenhaRejectsBlankHash() {
        User user = User.create("Ana Silva", "ana@example.com", "ana.silva", "old-hash", null, USER_TYPE_ID);

        assertThatThrownBy(() -> user.alterarSenha(" ")).isInstanceOf(DomainValidationException.class);
    }
}
