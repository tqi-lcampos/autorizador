# autorizador — Mini Autorizador

API REST que processa transações de Vale Refeição / Vale Alimentação vindas das maquininhas de cartão: cria cartões, informa o saldo e autoriza (ou recusa) as transações.

Contrato dos serviços em [`.claude/autorizador.md`](.claude/autorizador.md).

## Stack

| Item | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.1 |
| Arquitetura | MVC (Controller → Service → Repository → Model) |
| Banco | MySQL 8.4 (container) |
| Migrations | Flyway |
| Documentação | Swagger / OpenAPI 3 |
| Testes | JUnit 5, Mockito, MockMvc, H2 |

## Arquitetura MVC

```
src/main/java/com/vr/autorizador/
├── controller/            # Camada de entrada HTTP
│   ├── CardController         POST /cartoes, GET /cartoes/{numeroCartao}
│   ├── TransactionController  POST /transacoes
│   └── dto/                   CardRequestDTO, CardResponseDTO, TransactionRequestDTO, DeclineReason
├── service/               # Regras de negócio
│   ├── CardService            Criação do cartão (saldo inicial) e consulta de saldo
│   └── AuthorizationService   Regras de autorização, débito e idempotência
├── repository/            # Acesso a dados (Spring Data JPA)
├── model/                 # Entidades: Card, CardTransaction
├── exception/             # Exceções de negócio + @RestControllerAdvice
└── config/                # OpenAPI
```

O código, as entidades e as colunas do banco (`card`, `card_transaction`) estão em inglês.
O contrato HTTP permanece em português, como exige o enunciado: os endpoints `/cartoes` e
`/transacoes`, os campos JSON e os corpos de recusa. A tradução acontece nas fronteiras, e
**alterar qualquer nome da coluna da esquerda quebra o contrato**:

| JSON / corpo da resposta | Java | Onde é mapeado |
|---|---|---|
| `numeroCartao` | `cardNumber` | `@JsonProperty` em `CardRequestDTO`, `CardResponseDTO` e `TransactionRequestDTO` |
| `senha` | `password` | `@JsonProperty` em `CardRequestDTO` (só entrada) |
| `senhaCartao` | `cardPassword` | `@JsonProperty` em `TransactionRequestDTO` |
| `valor` | `amount` | `@JsonProperty` em `TransactionRequestDTO` |
| `{numeroCartao}` na URL | `cardNumber` | `@PathVariable("numeroCartao")` em `CardController` |
| `SALDO_INSUFICIENTE` | `INSUFFICIENT_BALANCE` | `DeclineReason#getCode()` |
| `SENHA_INVALIDA` | `INVALID_PASSWORD` | `DeclineReason#getCode()` |
| `CARTAO_INEXISTENTE` | `NONEXISTENT_CARD` | `DeclineReason#getCode()` |

O enum guarda o texto do contrato em `code` justamente para que renomear a constante Java
não altere o corpo da resposta.

**Divergência consciente do enunciado:** as respostas 201 e 422 de `POST /cartoes` devolvem
apenas `{"numeroCartao": "..."}`, via `CardResponseDTO`. O enunciado especifica o eco do corpo
recebido (com `senha`), mas devolver a senha em uma resposta HTTP a expõe em logs, proxies e
histórico do cliente sem nenhum ganho para o chamador, que acabou de enviá-la.

## Como subir

### Com Docker (aplicação + MySQL)

```bash
docker compose up --build
```

- Aplicação: <http://localhost:3003>
- Swagger: <http://localhost:3003/swagger-ui>
- MySQL: `localhost:3308` (`autorizadordb` / `appuser` / `apppassword`)

### Local (apenas o banco em container)

```bash
docker compose up -d mysql_autorizador
./mvnw spring-boot:run
```

### Testes

```bash
./mvnw test                        
```

## Endpoints

### `POST /cartoes` — cria um cartão

Todo cartão é criado com saldo inicial de **R$ 500,00**.

```bash
curl -X POST http://localhost:3003/cartoes \
  -H 'Content-Type: application/json' \
  -d '{ "numeroCartao": "6549873025634501", "senha": "1234" }'
```

| Status | Corpo | Quando |
|---|---|---|
| `201` | `{ "numeroCartao": "6549873025634501" }` | Cartão criado |
| `422` | `{ "numeroCartao": "6549873025634501" }` | Cartão já existe |
| `400` | JSON de erro | Corpo inválido (número fora do padrão de 16 dígitos, senha vazia) |

### `GET /cartoes/{numeroCartao}` — saldo do cartão

```bash
curl http://localhost:3003/cartoes/6549873025634501
```

| Status | Corpo | Quando |
|---|---|---|
| `200` | saldo (`495.15`) | Cartão encontrado |
| `404` | *sem corpo* | Cartão inexistente |

### `POST /transacoes` — autoriza uma transação

```bash
curl -X POST http://localhost:3003/transacoes \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 8b1f...'   # opcional \
  -d '{ "numeroCartao": "6549873025634501", "senhaCartao": "1234", "valor": 10.00 }'
```

| Status | Corpo | Quando |
|---|---|---|
| `201` | `OK` | Transação autorizada e valor debitado |
| `422` | `CARTAO_INEXISTENTE` | O cartão não existe |
| `422` | `SENHA_INVALIDA` | Senha diferente da senha do cartão |
| `422` | `SALDO_INSUFICIENTE` | Valor maior que o saldo disponível |
| `400` | JSON de erro | Corpo inválido (valor ausente ou ≤ 0) |

## Regras implementadas

Uma transação é autorizada quando o cartão existe, a senha está correta e há saldo disponível — verificados nessa ordem. Autorizada, o valor é debitado do saldo e a transação é registrada.

### Idempotência e concorrência

São duas garantias distintas, e só a primeira é obrigatória pelo contrato:

- **Concorrência entre instâncias** (obrigatória): o cartão é lido com **`SELECT ... FOR UPDATE`** (`@Lock(PESSIMISTIC_WRITE)`) dentro da transação, então duas instâncias da aplicação não debitam o mesmo saldo simultaneamente. Como o lock é do banco, vale para N instâncias. A entidade `Card` também tem `@Version`, que protege caminhos futuros que alterem o cartão sem pegar esse lock.
- **Deduplicação de reenvios** (opcional): a chave vem do header `Idempotency-Key` e é gravada em `card_transaction.idempotency_key`, com unique constraint. O reenvio com a mesma chave responde `201 OK` **sem debitar novamente**.

Em ambos os serviços a **unique constraint é o árbitro final** das corridas que o `SELECT` prévio não consegue enxergar — duas requisições simultâneas fazem o `exists...` antes de qualquer commit, então as duas o veem como falso e seguem para o `INSERT`. Quem perde a corrida recebe `DataIntegrityViolationException`, e cada serviço a traduz para o resultado correto:

| Corrida | Constraint | Tratamento |
|---|---|---|
| Duas instâncias criando o mesmo cartão | `uk_card_card_number` | `CardService` converte em `CardAlreadyExistsException` → `422` com o número do cartão, igual ao caminho sem corrida |
| Reenvio simultâneo da mesma transação | `uk_card_transaction_idempotency_key` | `AuthorizationService` lança `TransactionAlreadyProcessedException`; o `TransactionController` a captura e responde `201 OK`, pois a transação original já debitou o saldo |

Por isso o `saveAndFlush` (e não `save`) nos dois serviços: o flush força o `INSERT` ainda dentro do bloco `try`, onde a violação pode ser capturada e traduzida, em vez de estourar no commit da transação e virar `500`.

A chave é gerada **pelo cliente**, uma por intenção de venda, e reenviada em toda tentativa até haver resposta definitiva. Sem o header, `idempotencyKey` é `null`: a transação é debitada, a coluna fica nula (a unique constraint ignora nulos) e um `WARN` registra que aquele débito não tem proteção contra reenvio. A aplicação **não** gera a chave — uma chave gerada no servidor seria diferente em cada tentativa e daria uma falsa sensação de idempotência.

Limitação conhecida: o reenvio não valida se o corpo é o mesmo da chamada original. Um cliente que reutilize uma chave para outro valor recebe `201 OK` sem débito. A correção é comparar `card_number`/`amount` gravados com os da requisição e recusar quando divergirem.

## Premissas assumidas

1. **Resposta síncrona.** O contrato exige que o motivo da recusa volte na mesma chamada (`201 OK` / `422 <motivo>`), então a decisão de autorização é síncrona. `AuthorizationService.publishEvent` é o ponto de extensão para publicar a transação autorizada em Kafka (auditoria/integração) **após** o commit, fora do caminho da decisão — a chave da mensagem deve ser o número do cartão, para preservar a ordem por cartão.

2. **Senha armazenada como texto.** Não há requisito de hash no contrato. Em produção, a senha deveria ser armazenada com hash. A resposta de `POST /cartoes` já não devolve a senha (ver a divergência na seção de arquitetura).

3. **Valor da transação deve ser positivo.** O contrato não define o comportamento para valor zero ou negativo; a validação recusa com `400`.

4. **Cartão inexistente tem dois status.** `404` sem corpo na consulta de saldo e `422 CARTAO_INEXISTENTE` na transação, exatamente como o contrato descreve — por isso existem duas exceções (`CardNotFoundException` e `NonexistentCardException`).

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `card` | Número (único), senha, saldo e versão para lock otimista |
| `card_transaction` | Transações autorizadas: cartão, valor, chave de idempotência (única, opcional) e data |

Sobre `card_transaction.idempotency_key`: é a única coluna **`UNIQUE` sem `NOT NULL`** do schema, e
a combinação é intencional. Ela só existe quando o cliente envia o header `Idempotency-Key`, e a
unique constraint não compara valores nulos entre si (comportamento padrão do SQL, válido no MySQL
e no H2), então N transações sem chave convivem na tabela sem colidir. Acrescentar `NOT NULL` ali
faria toda transação sem header falhar no `INSERT`.

`created_at` é preenchido no `@PrePersist` de `CardTransaction` quando vem nulo, e não por
`DEFAULT` no banco, para que o valor seja o mesmo visto pela aplicação e pelos testes.
