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
docker compose up --build
```

Sobe com valores padrão de desenvolvimento definidos no próprio
`docker-compose.yml` — nenhum `.env` é necessário. Para usar um segredo JWT
ou credenciais de banco próprios, `cp .env.example .env` e edite antes de
subir (o Compose carrega `.env` automaticamente se ele existir).

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

## Coleção Postman

Em [`postman/`](postman/): `RestaurantManagement.postman_collection.json` +
`RestaurantManagement.postman_environment.json`.

1. Suba a aplicação (Docker Compose ou localmente).
2. No Postman: **Import** os dois arquivos.
3. Selecione o environment "Restaurant Management - Fase 2" (canto superior
   direito).
4. Abra a coleção → **Run** (Collection Runner) → Run Restaurant Management.

Roda do início ao fim sem nenhum passo manual, contra um banco vazio: cada
pasta encadeia o estado da anterior via variáveis de ambiente
(`userId`, `token`, `restaurantId`, `menuItemId`, ...) capturadas nos scripts
de teste de cada requisição. As únicas duas UUIDs fixas na coleção são os
tipos de usuário semeados (`donoTypeId`/`clienteTypeId`, documentados no
próprio arquivo de environment). Pode ser executada mais de uma vez contra o
mesmo banco sem falhar — um script de pré-requisição no nível da coleção
gera um sufixo único por execução, usado em todo e-mail/login que precisa
ser único.

Estrutura: uma pasta por agregado (`Auth`, `Users`, `UserTypes`,
`Restaurants`, `MenuItems`) demonstrando CRUD completo, mais uma pasta final
`Regras de negocio e erros` cobrindo os casos que diferenciam este projeto
(401/403/404/409/422/400 — incluindo a armadilha de rota aninhada do
`MenuItem`, que deve devolver 404 e nunca 403).

`bash scripts/audit.sh` (seção 10) falha o build se um endpoint mudar de
rota e a coleção não for atualizada junto.

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
