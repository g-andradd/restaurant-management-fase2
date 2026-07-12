package br.com.fiap.restaurant.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Restaurant {

    private final UUID id;
    private String nome;
    private String endereco;
    private TipoCozinha tipoCozinha;
    private HorarioFuncionamento horarioFuncionamento;
    private final UUID ownerId;
    private final LocalDateTime dataCriacao;
    private LocalDateTime dataUltimaAlteracao;

    private Restaurant(UUID id, String nome, String endereco, TipoCozinha tipoCozinha,
                        HorarioFuncionamento horarioFuncionamento, UUID ownerId,
                        LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        validarNome(nome);
        validarEndereco(endereco);
        this.nome = nome;
        this.endereco = endereco;
        this.tipoCozinha = Objects.requireNonNull(tipoCozinha, "tipoCozinha must not be null");
        this.horarioFuncionamento = Objects.requireNonNull(horarioFuncionamento, "horarioFuncionamento must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.dataCriacao = Objects.requireNonNull(dataCriacao, "dataCriacao must not be null");
        this.dataUltimaAlteracao = Objects.requireNonNull(dataUltimaAlteracao, "dataUltimaAlteracao must not be null");
    }

    public static Restaurant create(String nome, String endereco, TipoCozinha tipoCozinha,
                                     HorarioFuncionamento horarioFuncionamento, UUID ownerId) {
        var now = LocalDateTime.now();
        return new Restaurant(UUID.randomUUID(), nome, endereco, tipoCozinha, horarioFuncionamento, ownerId, now, now);
    }

    public static Restaurant reconstitute(UUID id, String nome, String endereco, TipoCozinha tipoCozinha,
                                          HorarioFuncionamento horarioFuncionamento, UUID ownerId,
                                          LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
        return new Restaurant(id, nome, endereco, tipoCozinha, horarioFuncionamento, ownerId, dataCriacao, dataUltimaAlteracao);
    }

    /**
     * ownerId is intentionally not a parameter here: ownership is fixed at
     * creation. Transferring a restaurant to a different owner is out of
     * scope for M04 - there is no use case for it.
     */
    public void atualizarDados(String nome, String endereco, TipoCozinha tipoCozinha,
                                HorarioFuncionamento horarioFuncionamento) {
        validarNome(nome);
        validarEndereco(endereco);
        this.nome = nome;
        this.endereco = endereco;
        this.tipoCozinha = Objects.requireNonNull(tipoCozinha, "tipoCozinha must not be null");
        this.horarioFuncionamento = Objects.requireNonNull(horarioFuncionamento, "horarioFuncionamento must not be null");
        this.dataUltimaAlteracao = LocalDateTime.now();
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome must not be blank");
        }
    }

    private static void validarEndereco(String endereco) {
        if (endereco == null || endereco.isBlank()) {
            throw new IllegalArgumentException("endereco must not be blank");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public TipoCozinha getTipoCozinha() {
        return tipoCozinha;
    }

    public HorarioFuncionamento getHorarioFuncionamento() {
        return horarioFuncionamento;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataUltimaAlteracao() {
        return dataUltimaAlteracao;
    }
}
