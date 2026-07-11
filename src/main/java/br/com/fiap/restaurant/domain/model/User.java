package br.com.fiap.restaurant.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class User {

    private final UUID id;
    private String nome;
    private String email;
    private String login;
    private String senhaHash;
    private String endereco;
    private UUID userTypeId;
    private final LocalDateTime dataCriacao;
    private LocalDateTime dataUltimaAlteracao;

    private User(UUID id, String nome, String email, String login, String senhaHash,
                 String endereco, UUID userTypeId, LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        validarNome(nome);
        validarEmail(email);
        validarLogin(login);
        validarSenhaHash(senhaHash);
        this.nome = nome;
        this.email = email;
        this.login = login;
        this.senhaHash = senhaHash;
        this.endereco = endereco;
        this.userTypeId = Objects.requireNonNull(userTypeId, "userTypeId must not be null");
        this.dataCriacao = Objects.requireNonNull(dataCriacao, "dataCriacao must not be null");
        this.dataUltimaAlteracao = Objects.requireNonNull(dataUltimaAlteracao, "dataUltimaAlteracao must not be null");
    }

    public static User create(String nome, String email, String login, String senhaHash,
                               String endereco, UUID userTypeId) {
        var now = LocalDateTime.now();
        return new User(UUID.randomUUID(), nome, email, login, senhaHash, endereco, userTypeId, now, now);
    }

    public static User reconstitute(UUID id, String nome, String email, String login, String senhaHash,
                                     String endereco, UUID userTypeId, LocalDateTime dataCriacao,
                                     LocalDateTime dataUltimaAlteracao) {
        return new User(id, nome, email, login, senhaHash, endereco, userTypeId, dataCriacao, dataUltimaAlteracao);
    }

    public void atualizarDados(String nome, String email, String login, String endereco, UUID userTypeId) {
        validarNome(nome);
        validarEmail(email);
        validarLogin(login);
        this.nome = nome;
        this.email = email;
        this.login = login;
        this.endereco = endereco;
        this.userTypeId = Objects.requireNonNull(userTypeId, "userTypeId must not be null");
        this.dataUltimaAlteracao = LocalDateTime.now();
    }

    public void alterarSenha(String novaSenhaHash) {
        validarSenhaHash(novaSenhaHash);
        this.senhaHash = novaSenhaHash;
        this.dataUltimaAlteracao = LocalDateTime.now();
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome must not be blank");
        }
    }

    private static void validarEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("email must be a non-blank address containing '@'");
        }
    }

    private static void validarLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login must not be blank");
        }
    }

    private static void validarSenhaHash(String senhaHash) {
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new IllegalArgumentException("senhaHash must not be blank");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getEndereco() {
        return endereco;
    }

    public UUID getUserTypeId() {
        return userTypeId;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataUltimaAlteracao() {
        return dataUltimaAlteracao;
    }
}
