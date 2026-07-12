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

## 2026-07-08 — M02: Autenticação JWT

- **JJWT já estava no `pom.xml` desde o M00, sem uso até agora**: o M02 não
  adicionou nenhuma dependência nova — só passou a usar `jjwt-api`/
  `jjwt-impl`/`jjwt-jackson` 0.12.6 que já estavam declarados.
- **`application-test.yml` já tinha um segredo de JWT** (adicionado no
  ajuste de perfil de teste do M00), com 40 caracteres (320 bits) — acima
  do mínimo de 256 bits exigido por HS256. Nenhuma mudança necessária ali.
- **Bug latente encontrado no `application-dev.yml`**: o valor padrão de
  fallback `${JWT_SECRET:change-me-dev-only-not-for-prod}` tinha só 31
  caracteres (248 bits) — abaixo do mínimo de 256 bits do HS256. Era inerte
  até agora (JJWT não era usado), mas quebraria `./mvnw spring-boot:run`
  local sem um `.env` carregado assim que a assinatura de token entrasse
  em uso. Corrigido para 35 caracteres.
- **`InvalidCredentialsException`/`InvalidTokenException` em
  `application/exception`, não em `domain.exception`**: a família
  `domain.exception` existente representa violações de invariantes do
  próprio agregado `User` (email/login duplicado, não encontrado). "Senha
  errada"/"token inválido" são falhas da orquestração do caso de uso de
  login, não invariantes que o `User` conhece — por isso ficam na camada
  de aplicação, como `RuntimeException` simples (não estendem
  `DomainException`).
- **`sub` do JWT é o UUID do usuário, não o `login`**: `login` é mutável
  (`User.atualizarDados` permite trocá-lo), então usar o `id` imutável como
  `sub` mantém o token válido mesmo que o login mude depois de emitido. O
  `login` ainda vai como claim customizado para legibilidade/debug.
- **`Keys.hmacShaKeyFor` do próprio JJWT valida o tamanho mínimo da
  chave** — não foi implementada nenhuma checagem manual de tamanho;
  `WeakKeyException` (não verificada) já falha o startup do contexto
  Spring com uma mensagem clara se o segredo for curto demais.
- **`JwtAuthenticationFilter` não é `@Component`**: é construído
  diretamente dentro de `SecurityConfig` (recebendo `TokenProvider` já
  injetado). Anotar um `Filter` com `@Component` faria o Spring Boot
  registrá-lo automaticamente como filtro de servlet global, executando-o
  duas vezes (uma via a chain do Spring Security, outra via o
  registration bean automático) — problema clássico e evitado aqui.
- **`AuthenticationEntryPoint`/`AccessDeniedHandler` escrevem
  `ProblemDetail` manualmente**: `AuthenticationException`/
  `AccessDeniedException` são lançadas dentro da própria chain de filtros
  do Spring Security (pelo `AuthorizationFilter`) e capturadas ali mesmo
  pelo `ExceptionTranslationFilter`, que delega para essas duas classes —
  tudo dentro da camada de filtros de servlet, antes de a requisição
  chegar ao `DispatcherServlet`. Por isso não são métodos
  `@ExceptionHandler`: `@RestControllerAdvice` só intercepta exceções
  lançadas durante o dispatch normal do Spring MVC, que esse caminho nunca
  alcança. Ambas serializam `ProblemDetail` via `ObjectMapper` injetado,
  mantendo o mesmo formato (`type`/`title`/`status`/`detail`) usado pelo
  `GlobalExceptionHandler`.
- **Swagger/`api-docs` deixados `permitAll()` deliberadamente**: decisão
  consciente para facilitar a avaliação acadêmica do projeto (acesso
  direto ao Swagger UI sem precisar logar antes). Revisitar antes de
  qualquer exposição real do serviço — normalmente essas rotas também
  deveriam ficar atrás de autenticação ou de uma rede restrita.

## 2026-07-10 — fix/M02: deadlock de bootstrap (P0) — auto-registro público

- **Bug P0 encontrado por auditoria externa, não pelos testes**:
  `SecurityConfig` tinha `.anyRequest().authenticated()`, mas o único jeito
  de conseguir um token era logar como um usuário já existente — e não
  havia nenhum usuário semeado nem nenhuma rota pública para criar um. Uma
  instância recém-implantada ficava permanentemente inutilizável: todo
  endpoint (incluindo `POST /api/v1/users`) devolvia 401, e não havia
  nenhuma forma de sair desse estado sem inserir um usuário manualmente no
  banco.
- **Por que o build ficava verde mesmo assim**: `AuthenticationIntegrationTest`
  semeava seu usuário chamando `createUserUseCase.execute(...)` diretamente
  em Java, pulando a camada HTTP inteira — nenhum teste jamais passava pela
  porta trancada (a chain de filtros do Spring Security) para tentar criar
  o primeiro usuário via API. A cobertura de 92%+ e o `BUILD SUCCESS`
  mediam código exercitado, não o fluxo de bootstrap real de uma instância
  nova. Corrigido adicionando um teste "cold start"
  (`coldStartSelfRegistrationWorksThroughHttpAloneWithNoDirectSeeding`) que
  só usa `MockMvc`, nunca o use case diretamente.
- **Decisão: `POST /api/v1/users` é auto-registro público**
  (`permitAll()`), declarado ANTES de `anyRequest().authenticated()` na
  chain — `GET`/`PUT`/`DELETE` em `/api/v1/users/**` continuam exigindo
  token.
- **Flag para o M03**: quando `UserType` existir, decidir se um usuário
  que se auto-registra pode se declarar "Dono de Restaurante" diretamente
  no payload de criação (risco de escalada de privilégio — qualquer um
  vira dono de restaurante sem nenhuma verificação) ou se todo
  auto-registro entra como "Cliente" por padrão, com a promoção a "Dono de
  Restaurante" exigindo um fluxo separado/mais controlado.

## 2026-07-11 — M03: UserType (Tipo de Usuário)

- **Decisão (b) mantida como estava, com a ressalva registrada
  honestamente**: o tipo é escolhido livremente no auto-registro,
  incluindo "Dono de Restaurante", sem role de admin e sem fluxo de
  aprovação — fora do escopo da Fase 2. Considerou-se forçar "Cliente" no
  signup e permitir troca só por um usuário já autenticado, mas isso seria
  teatro de segurança (qualquer usuário autenticado ainda poderia se
  promover livremente) — e uma role ADMIN de verdade reintroduziria o
  mesmo problema de bootstrap do fix/M02 (quem cria o primeiro admin?).
  Preferimos documentar a limitação claramente a fingir um controle que
  não existe de fato.
- **`UserType` é agregado próprio, `User` referencia por `userTypeId`
  (UUID) só**: o brief exige CRUD independente de `UserType`
  (`GET /user-types`, etc., sem nenhum `User` envolvido) — sinal clássico
  de que precisa de identidade e ciclo de vida próprios, não é um VO
  dentro de `User`. Referência só por id segue a mesma disciplina que
  `Restaurant → User` (owner) já usa no spec de produto.
- **`user_type_id` NOT NULL, com backfill para "Cliente"**: three-step
  migration (add nullable → backfill → set NOT NULL) no V3, mais seguro
  para uma tabela que pode já ter linhas. Backfill para "Cliente" (não
  "Dono de Restaurante") é o padrão conservador — ninguém vira dono de
  restaurante silenciosamente por causa de uma migration.
- **Sem endpoint dedicado para associar tipo a usuário existente**:
  reaproveita `PUT /api/v1/users/{id}` (agora com `userTypeId`
  obrigatório), cobrindo "associar a um usuário existente" sem rota nova —
  YAGNI.
- **`UserResponse`/`UserResult` embutem `{ id, nome }` do tipo, não só o
  id** (override do plano original): `GetUserByIdUseCase` e
  `ListUsersUseCase` passaram a depender de `UserTypeRepository` por causa
  disso. Para evitar N+1 na listagem, `UserTypeRepository` ganhou
  `findAllById(Collection<UUID>)` e `ListUsersUseCase` resolve os tipos
  distintos da página inteira em UMA chamada, nunca um `findById` por
  usuário — testado explicitamente (`ListUsersUseCaseTest` verifica que
  `findAllById` é chamado exatamente uma vez).
- **`userTypeId` desconhecido no corpo de `POST`/`PUT /users` → 422, não
  404** (override do plano original): 404 seria ambíguo — o cliente pode
  ler como "o endpoint /users não existe". Nova exceção
  `InvalidUserTypeReferenceException` (em `domain.exception`, mas
  lançada pelos use cases de `User`) mapeia para 422. `UserTypeNotFoundException`
  → 404 continua existindo, mas só para `GET`/`PUT`/`DELETE
  /api/v1/user-types/{id}`, onde o recurso ausente É o alvo da própria
  URL. **Ver correção datada de 2026-07-12 abaixo**: essa regra
  inicialmente só era garantida pelo fallback genérico de
  `DomainException`, sem handler nem teste dedicados.
- **`GET /api/v1/user-types/**` é público**: sem isso, um auto-registro
  anônimo não teria como descobrir ids de tipo válidos via API (só via
  documentação/UUID fixo). `POST`/`PUT`/`DELETE` continuam autenticados.
- **Armadilha de corretude documentada para o M04**: a claim `"userType"`
  no JWT é um snapshot do momento do login — `JwtAuthenticationFilter`
  nunca consulta o banco por requisição (decisão do M02), então, se o tipo
  do usuário for trocado depois do login, o token antigo continua com o
  nome do tipo velho até expirar. Qualquer decisão de autorização (a regra
  "só Dono de Restaurante pode ser dono de restaurante" do M04, em
  especial) TEM que reler o tipo atual do banco via
  `UserRepository`/`UserTypeRepository` — nunca confiar nessa claim para
  além de exibição/conveniência. Documentado no Javadoc de
  `AuthenticateUserUseCase` e em `specs/modules/03-user-type.md`.
- **Limitação conhecida, aceita e não corrigida**: apagar todos os
  `UserType` enquanto nenhum `User` existir quebraria o auto-registro (não
  haveria `userTypeId` válido para o signup público criar o primeiro
  usuário, e criar um novo tipo via `POST /api/v1/user-types` exige
  token). Isso exige uma ação destrutiva deliberada (apagar os dois tipos
  semeados) e só é possível enquanto nenhum usuário existe — assim que
  existir ao menos um usuário, `DeleteUserTypeUseCase` bloqueia a exclusão
  do tipo em uso (409). Cenário de baixíssima probabilidade, documentado
  aqui em vez de resolvido com mais complexidade (ex.: proteger os dois
  tipos semeados contra exclusão).

## 2026-07-12 — fix/M03: 422 sem handler nem teste próprios

- **Achado de auditoria**: `InvalidUserTypeReferenceException` já
  devolvia 422 corretamente, mas só por cair no fallback genérico
  `@ExceptionHandler(DomainException.class)` (o mesmo que o M01 introduziu
  como rede de segurança para exceções de domínio futuras sem handler
  específico) — e nenhum teste em toda a suíte chamava
  `isUnprocessableEntity()`. A decisão deliberada "422, não 404" estava
  protegida por nada: bastaria o M04 adicionar ou reordenar um handler de
  exceção de `Restaurant` para mudar esse status silenciosamente, sem
  quebrar nenhum teste.
- **Regra explícita agora, ver `specs/modules/03-user-type.md`**: um
  `UserType` inexistente é 404 quando é o próprio recurso endereçado pela
  URL, e 422 quando é uma referência dentro do corpo de uma requisição.
  Corrigido com: (1) `GlobalExceptionHandler.handleInvalidUserTypeReference`
  — handler dedicado para `InvalidUserTypeReferenceException`, não mais
  dependente do fallback genérico; (2) testes que afirmam
  `isUnprocessableEntity()` explicitamente em `UserControllerTest`
  (create/update com `userTypeId` inexistente) e no round-trip HTTP
  completo em `UserTypeIntegrationTest`
  (`publicSignupWithNonExistentUserTypeIdReturns422`); (3) o teste de 404
  em `UserTypeControllerTest.getByIdReturns404WhenNotFound` reforçado com
  as mesmas asserções de forma (`type`/`title`/`status`/`detail`) para
  deixar o contraste 404-vs-422 direto de comparar.
- **Padrão a repetir no M04**: qualquer referência cross-aggregate nova
  (ex.: `Restaurant.ownerId` apontando para um `User` inexistente) deveria
  seguir o mesmo molde — exceção dedicada + handler explícito + teste
  afirmando o status específico, em vez de confiar no fallback genérico.

## 2026-07-11 — M04: Restaurant

- **Autorização por capability flag, nunca por nome/UUID do tipo**:
  `UserType` ganhou `podeSerDono` (coluna `can_own_restaurant`, backfill
  `TRUE` só para o "Dono de Restaurante" semeado). `CreateRestaurantUseCase`
  checa `ownerType.podeSerDono()`, nunca `nome.equals("Dono de
  Restaurante")` nem comparação com o UUID fixo — um rename futuro do tipo,
  ou um novo tipo com permissão de dono, continuam funcionando sem tocar
  em código de autorização.
- **Bug P0 encontrado pelo usuário antes de qualquer teste rodar (mesma
  classe do fix/M02)**: o plano original de `CreateRestaurantUseCase`
  resolvia o `owner` a partir de `command.ownerId()` e checava só se o
  *tipo* dele podia ser dono — sem nunca comparar `ownerId` com o id do
  chamador autenticado. Isso significava que qualquer usuário logado
  (mesmo um Cliente) podia criar um restaurante em nome de outra pessoa,
  bastando saber o id dela, desde que essa pessoa fosse de um tipo que
  pode ser dono. Corrigido adicionando a checagem
  `command.ownerId().equals(authenticatedUserProvider.getCurrentUserId())`
  **antes** da checagem de capability, lançando `NotRestaurantOwnerException`
  → 403 em caso de divergência. Não existe role de admin na Fase 2, então
  não há nenhum caso legítimo de "criar em nome de outro usuário" — a
  checagem não precisa de exceção para esse cenário.
- **`AuthenticatedUserProvider` como porta nova**: use cases precisavam
  saber "quem está chamando" sem depender de `SecurityContextHolder`
  diretamente (isso violaria a regra de `application` não depender de
  Spring). Porta em `application.port`, implementação
  (`SpringSecurityAuthenticatedUserProvider`) em `infrastructure.security`,
  lendo o principal do contexto de segurança e fazendo `UUID.fromString`.
- **Update/delete releem `ownerId` do banco a cada chamada, nunca
  confiam em cache**: `UpdateRestaurantUseCase`/`DeleteRestaurantUseCase`
  buscam o restaurante, comparam `restaurant.getOwnerId()` com
  `authenticatedUserProvider.getCurrentUserId()` (id imutável do token,
  não a claim `"userType"`) e só então prosseguem. Mismatch →
  `NotRestaurantOwnerException` → 403.
- **AC6 tornado comportamental, não estrutural**: o plano original ia
  provar a armadilha da claim `"userType"` (documentada no M03) só de
  forma estrutural (ex.: "o código nunca lê essa claim"). Substituído por
  `RestaurantIntegrationTest.staleTokenClaimDoesNotGrantOwnership`: cria um
  Dono, loga (guarda o token), rebaixa esse mesmo usuário para Cliente via
  `PUT /api/v1/users/{id}` (escolhido em vez de `PUT /user-types/{id}`
  para não mexer nos dois tipos semeados compartilhados por outros
  testes), e reusa o token ANTIGO — ainda com a claim `"Dono de
  Restaurante"` — para tentar criar um restaurante. Resultado tem que ser
  422 (regra de negócio: o tipo *atual* do usuário não pode ser dono), não
  201. Prova de verdade que a autorização relê o banco a cada chamada, não
  confia no token.
- **`DeleteUserUseCase` ganhou checagem de restaurantes antes de apagar**:
  um usuário que ainda é dono de pelo menos um restaurante não pode ser
  apagado (`UserOwnsRestaurantsException` → 409), via
  `RestaurantRepository.existsByOwnerId` — evita uma violação de FK
  aparecendo como 500 cru.
- **Limitação aceita e documentada, não corrigida**: `HorarioFuncionamento`
  exige `abertura` estritamente antes de `fechamento` no mesmo relógio, ou
  seja, não dá para representar um horário que atravessa a meia-noite
  (ex.: 18:00–02:00). Modelar isso direito (rollover de dia, consultas de
  "está aberto agora" cruzando meia-noite) é complexidade real sem
  requisito de produto puxando por ela na Fase 2. Ver
  `specs/modules/04-restaurant.md`.
- **`ArchitecturePurityTest.mustNotUsePreAuthorizeAnnotations`**: nenhum
  método em todo o projeto pode usar `@PreAuthorize` — a regra de
  ownership é código de use case, testável e explícito, não uma anotação
  de framework que esconderia a regra dos testes unitários. Mesma
  motivação do "fora de escopo" já registrado no M03 para autorização
  baseada em role.
- **`GlobalExceptionHandler` ganhou handler para
  `HttpMessageNotReadableException` → 400**: um literal de enum
  `tipoCozinha` desconhecido no corpo da requisição falha durante a
  desserialização do Jackson, antes mesmo do Bean Validation rodar — sem
  esse handler, cairia no fallback genérico de 500.
