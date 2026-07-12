package br.com.fiap.restaurant.domain.model;

import br.com.fiap.restaurant.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class MenuItem {

    private final UUID id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private boolean disponivelSomenteNoLocal;
    private String fotoPath;
    private final UUID restaurantId;
    private final LocalDateTime dataCriacao;
    private LocalDateTime dataUltimaAlteracao;

    private MenuItem(UUID id, String nome, String descricao, BigDecimal preco, boolean disponivelSomenteNoLocal,
                      String fotoPath, UUID restaurantId, LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        validarNome(nome);
        validarPreco(preco);
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.disponivelSomenteNoLocal = disponivelSomenteNoLocal;
        this.fotoPath = fotoPath;
        this.restaurantId = Objects.requireNonNull(restaurantId, "restaurantId must not be null");
        this.dataCriacao = Objects.requireNonNull(dataCriacao, "dataCriacao must not be null");
        this.dataUltimaAlteracao = Objects.requireNonNull(dataUltimaAlteracao, "dataUltimaAlteracao must not be null");
    }

    public static MenuItem create(String nome, String descricao, BigDecimal preco, boolean disponivelSomenteNoLocal,
                                   String fotoPath, UUID restaurantId) {
        var now = LocalDateTime.now();
        return new MenuItem(UUID.randomUUID(), nome, descricao, preco, disponivelSomenteNoLocal, fotoPath,
                restaurantId, now, now);
    }

    public static MenuItem reconstitute(UUID id, String nome, String descricao, BigDecimal preco,
                                         boolean disponivelSomenteNoLocal, String fotoPath, UUID restaurantId,
                                         LocalDateTime dataCriacao, LocalDateTime dataUltimaAlteracao) {
        return new MenuItem(id, nome, descricao, preco, disponivelSomenteNoLocal, fotoPath, restaurantId,
                dataCriacao, dataUltimaAlteracao);
    }

    /**
     * restaurantId is intentionally not a parameter here - same reasoning as
     * Restaurant.atualizarDados excluding ownerId: it's fixed at creation,
     * there is no "move this item to another restaurant" use case.
     */
    public void atualizarDados(String nome, String descricao, BigDecimal preco, boolean disponivelSomenteNoLocal,
                                String fotoPath) {
        validarNome(nome);
        validarPreco(preco);
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.disponivelSomenteNoLocal = disponivelSomenteNoLocal;
        this.fotoPath = fotoPath;
        this.dataUltimaAlteracao = LocalDateTime.now();
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainValidationException("nome must not be blank");
        }
    }

    private static void validarPreco(BigDecimal preco) {
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("preco must be greater than zero");
        }
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
