package br.com.fiap.restaurant.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
public class MenuItemJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "disponivel_somente_no_local", nullable = false)
    private boolean disponivelSomenteNoLocal;

    @Column(name = "foto_path")
    private String fotoPath;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_ultima_alteracao", nullable = false)
    private LocalDateTime dataUltimaAlteracao;

    protected MenuItemJpaEntity() {
        // required by JPA
    }

    public MenuItemJpaEntity(UUID id, String nome, String descricao, BigDecimal preco,
                              boolean disponivelSomenteNoLocal, String fotoPath, UUID restaurantId,
                              LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.disponivelSomenteNoLocal = disponivelSomenteNoLocal;
        this.fotoPath = fotoPath;
        this.restaurantId = restaurantId;
        this.dataCriacao = dataCriacao;
        this.dataUltimaAlteracao = dataUltimaAlteracao;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public boolean isDisponivelSomenteNoLocal() {
        return disponivelSomenteNoLocal;
    }

    public String getFotoPath() {
        return fotoPath;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataUltimaAlteracao() {
        return dataUltimaAlteracao;
    }
}
