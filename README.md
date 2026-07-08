# Restaurant Management System — Fase 2

Backend compartilhado para gestão de restaurantes, desenvolvido como Fase 2
do Tech Challenge de "Arquitetura e Desenvolvimento Java" da FIAP Pos Tech.
Permite que donos de restaurante gerenciem seus estabelecimentos e que
clientes naveguem por eles.

## Stack

- Java 21, Spring Boot 3.3.5, Maven (single-module)
- PostgreSQL + Flyway
- Spring Data JPA (camada de infraestrutura)
- Spring Security 6 + JWT (JJWT)
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit
- JaCoCo (gate de 80% de cobertura de linha)
- Docker + Docker Compose

## Arquitetura

Clean Architecture em três camadas, com a regra de dependência
`infrastructure -> application -> domain` verificada por um teste ArchUnit.
Detalhes em [`specs/technical/architecture.md`](specs/technical/architecture.md).

## Como rodar

### Com Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

A aplicação sobe em `http://localhost:8080`, com Swagger UI em
`http://localhost:8080/swagger-ui.html`.

### Localmente

```bash
# suba apenas o Postgres
docker compose up postgres -d

# rode a aplicação
./mvnw spring-boot:run
```

### Testes

```bash
./mvnw verify
```

Requer Docker em execução (os testes de integração usam Testcontainers).

## Especificações

O planejamento e as decisões técnicas de cada módulo estão em
[`specs/`](specs/):

- [`specs/product/overview.md`](specs/product/overview.md) — visão de produto e escopo.
- [`specs/technical/architecture.md`](specs/technical/architecture.md) — arquitetura e stack.
- [`specs/modules/`](specs/modules/) — um arquivo por módulo, com objetivo, entregáveis e critério de pronto.

## Convenção de commits

`feat(MXX):` / `chore(MXX):` / `docs(MXX):` / `test(MXX):`

## Branches

GitFlow: `main` (estável) + `develop` (integração).
