# NOTES.md — Log de decisões

## 2026-07-07 — M00: Bootstrap

- **Stack travada**: Java 21, Spring Boot 3.3.5, Maven single-module.
  PostgreSQL + Flyway, Spring Data JPA (infra), Spring Security 6 + JJWT
  0.12.6, springdoc-openapi 2.6.0. Testes: JUnit 5, Mockito, AssertJ,
  Testcontainers (Postgres), ArchUnit 1.3.0.
- **Pacote base**: `br.com.fiap.restaurant`, adotado sem alterações.
- **Versionamento**: propriedade `${revision}` + `flatten-maven-plugin`
  (`resolveCiFriendliesOnly`).
- **JaCoCo**: gate de 80% de cobertura de linha configurado desde já
  (`BUNDLE`/`LINE`/`COVEREDRATIO`), preso à fase `verify`. Com zero classes
  de produção no M00, a razão de cobertura do JaCoCo é vacuamente `1.0`
  (0/0), então o build fica verde sem nenhum hack de `haltOnFailure=false`.
  A partir do módulo com as primeiras entidades, esse gate passa a valer de
  verdade.
- **Dependência adicional não prevista na lista travada**:
  `spring-boot-testcontainers` (glue oficial do Spring Boot para
  `@ServiceConnection`), necessária para ligar o Testcontainers Postgres de
  forma limpa. Não é uma dependência de terceiros nova, é o módulo de
  integração do próprio Spring Boot — mas fica registrado aqui por
  transparência.
- **Baseline do Flyway**: optou-se por um `V1__baseline.sql` vazio agora, em
  vez de adiar a primeira migration para o módulo do agregado `User`. Isso
  prova o pipeline Flyway + Testcontainers + Postgres de ponta a ponta já no
  M00, com risco zero (arquivo vazio). O próximo módulo começa em `V2__...`.
- **Sem `SecurityConfig`/JWT wiring no M00**: `spring-boot-starter-security`
  autoconfigura um setup padrão inofensivo (sem endpoints expostos). A
  configuração de JWT/permitAll fica para o módulo que introduzir o primeiro
  controller.
- **Estado do repositório ao iniciar o M00**: `main` e `develop` já
  existiam no `origin` (mesmo commit `c39c39e`). Trabalho do M00 foi feito
  inteiramente em `develop`; `main` não foi tocado (GitFlow padrão — `main`
  só se move via merge de release).
- **Testcontainers 1.21.4 em vez de 1.20.4**: a máquina de desenvolvimento
  roda um Docker Desktop/Engine muito recente (API 1.53). O cliente
  docker-java embutido no Testcontainers 1.20.4 não conseguia negociar a
  API corretamente pelo named pipe do Windows (erro 400 com corpo vazio).
  Bump para 1.21.4 resolveu sem nenhuma outra mudança de configuração.
- **`.gitattributes`**: adicionado (fora da lista original de arquivos)
  para forçar `eol=lf` no `mvnw` — sem isso, `core.autocrlf=true` no
  Windows quebraria o script de shell dentro do estágio Linux do
  `Dockerfile`.
