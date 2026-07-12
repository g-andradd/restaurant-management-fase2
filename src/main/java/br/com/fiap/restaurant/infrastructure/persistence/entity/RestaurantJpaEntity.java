package br.com.fiap.restaurant.infrastructure.persistence.entity;

import br.com.fiap.restaurant.domain.model.TipoCozinha;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "restaurants")
public class RestaurantJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "endereco", nullable = false)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cozinha", nullable = false)
    private TipoCozinha tipoCozinha;

    @Column(name = "horario_abertura", nullable = false)
    private LocalTime horarioAbertura;

    @Column(name = "horario_fechamento", nullable = false)
    private LocalTime horarioFechamento;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_ultima_alteracao", nullable = false)
    private LocalDateTime dataUltimaAlteracao;

    protected RestaurantJpaEntity() {
        // required by JPA
    }

    public RestaurantJpaEntity(UUID id, String nome, String endereco, TipoCozinha tipoCozinha,
                                LocalTime horarioAbertura, LocalTime horarioFechamento, UUID ownerId,
                                LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
        this.tipoCozinha = tipoCozinha;
        this.horarioAbertura = horarioAbertura;
        this.horarioFechamento = horarioFechamento;
        this.ownerId = ownerId;
        this.dataCriacao = dataCriacao;
        this.dataUltimaAlteracao = dataUltimaAlteracao;
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

    public LocalTime getHorarioAbertura() {
        return horarioAbertura;
    }

    public LocalTime getHorarioFechamento() {
        return horarioFechamento;
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
