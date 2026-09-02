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
│   └── dto/                   CardRequestDTO, TransactionRequestDTO, DeclineReason
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
`/transacoes`, os campos JSON (`numeroCartao`, `senha`, `senhaCartao`, `valor`) — mapeados
nos DTOs via `@JsonProperty` — e os corpos de recusa (`SALDO_INSUFICIENTE`, `SENHA_INVALIDA`,
`CARTAO_INEXISTENTE`), expostos por `DeclineReason#getCode()`.

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
| `201` | eco do corpo enviado | Cartão criado |
| `422` | eco do corpo enviado | Cartão já existe |
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

- O cartão é lido com **`SELECT ... FOR UPDATE`** (`@Lock(PESSIMISTIC_WRITE)`) dentro da transação, então duas instâncias da aplicação não debitam o mesmo saldo simultaneamente. A entidade `Cartao` também tem `@Version`, como proteção adicional.
- Cada transação grava uma **chave de idempotência única** (`transacao.chave_idempotencia`). A chave vem do header opcional `Idempotency-Key`; sem o header, é gerado um UUID por requisição. O reenvio com a mesma chave responde `201 OK` **sem debitar novamente** — inclusive em reenvios simultâneos, barrados pela unique key.

## Premissas assumidas

1. **Resposta síncrona.** O contrato exige que o motivo da recusa volte na mesma chamada (`201 OK` / `422 <motivo>`), então a decisão de autorização é síncrona. `AutorizacaoService.publicarEvento` é o ponto de extensão para publicar a transação autorizada em Kafka (auditoria/integração) **após** o commit, fora do caminho da decisão — a chave da mensagem deve ser o número do cartão, para preservar a ordem por cartão.

2. **Senha armazenada como texto.** O contrato ecoa a senha na resposta de `POST /cartoes`; não há requisito de hash. Em produção, a senha deveria ser armazenada com hash e nunca devolvida.

3. **Valor da transação deve ser positivo.** O contrato não define o comportamento para valor zero ou negativo; a validação recusa com `400`.

4. **Cartão inexistente tem dois status.** `404` sem corpo na consulta de saldo e `422 CARTAO_INEXISTENTE` na transação, exatamente como o contrato descreve — por isso existem duas exceções (`CartaoNaoEncontradoException` e `CartaoInexistenteException`).

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `cartao` | Número (único), senha, saldo e versão para lock otimista |
| `transacao` | Transações autorizadas: cartão, valor, chave de idempotência (única) e data |
