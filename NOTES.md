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

## 2026-07-08 — M01: User (CRUD, sem autenticação)

- **Bug latente do M00 encontrado e corrigido**: `LayeredArchitectureTest`
  usava `.consideringAllDependencies()`, que passou despercebido no M00
  porque não havia nenhuma classe de produção (0 classes analisadas). Ao
  adicionar código real no M01, esse modo passou a contar QUALQUER
  dependência — inclusive para JDK/Spring/Mockito — como uma violação de
  `mayOnlyAccessLayers(...)`, gerando 957 "violações" falsas (ex.: `User`
  tendo um campo `LocalDateTime` já contava como violação). Trocado para
  `.consideringOnlyDependenciesInAnyPackage("br.com.fiap.restaurant..")`,
  que restringe a checagem a dependências reais entre as três camadas,
  ignorando corretamente JDK/bibliotecas de terceiros. A regra de pureza
  (zero Spring/JPA/Bean Validation no domain, zero Spring no application)
  agora é responsabilidade do novo `ArchitecturePurityTest`, que nomeia os
  pacotes-alvo explicitamente e não sofre desse problema.
- **`SecurityConfig` com `permitAll()` já no M01, não no M02**:
  `spring-boot-starter-security` já estava no `pom.xml` desde o M00; como o
  M01 introduz os primeiros endpoints reais, a autoconfiguração padrão do
  Spring Security passaria a exigir Basic Auth com senha gerada a cada
  restart. Sem esse `SecurityConfig`, os endpoints de CRUD ficariam
  inutilizáveis, contradizendo o requisito "sem autenticação" do módulo.
- **Trade-off aceito: `save()` com UUID atribuído pelo domínio causa
  SELECT-antes-do-INSERT**: como `User.create()` gera o UUID no próprio
  domínio (não é gerado pelo banco), o Spring Data JPA não consegue usar sua
  heurística padrão de "entidade nova = @Id nulo" — toda entidade chega ao
  `UserJpaRepository.save()` já com id preenchido, então o Hibernate usa
  `merge()` (que faz um SELECT para decidir entre INSERT/UPDATE) em vez de
  `persist()` direto. Fica um SELECT extra por `create`. Correção possível
  seria implementar `Persistable<UUID>` com uma flag transiente `isNew`, mas
  isso é adiado — o comportamento está correto (só o INSERT é mais caro que
  o necessário), e não vale a complexidade extra enquanto não virar gargalo
  real (ex.: import em lote).
- **`spring-security-crypto` declarado explicitamente**: já estava
  transitivamente no classpath via `spring-boot-starter-security` (mesma
  descoberta feita no planejamento, confirmada com
  `mvn dependency:tree -Dverbose`). A declaração explícita no `pom.xml` não
  adiciona nenhum jar novo — é só para não depender de uma transitiva não
  declarada para uma classe (`BCryptPasswordEncoder`) que importamos
  diretamente.
- **Mapper manual, não MapStruct**: mantido conforme preferência
  confirmada no planejamento — poucos campos, sem annotation processor
  extra.
- **Paginação sem `Pageable` em domain/application**: a porta
  `UserRepository` recebe `(int page, int size)` e expõe `count()`
  separadamente; `PageResult`/`PageQuery` são records em `application/dto`;
  a conversão para `PageRequest` do Spring Data acontece só dentro de
  `UserRepositoryAdapter`.
- **Use cases sem anotação Spring**: `application/usecase/*` são classes
  simples; `infrastructure/config/BeanConfiguration` é quem os expõe como
  `@Bean`, mantendo a camada de aplicação livre de dependências de
  framework (verificado por `ArchitecturePurityTest`).
