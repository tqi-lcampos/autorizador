# cfontes0estapar — Gestão de Estacionamento

API REST que consome os eventos do simulador de garagem, aplica preço dinâmico por lotação e apura o faturamento por setor e data.

Regras de negócio e contrato das APIs em [`.claude/.rule/rule.md`](.claude/.rule/rule.md).

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
src/main/java/com/fiap/cfontes0estapar/
├── controller/            # Camada de entrada HTTP (View da API)
│   ├── WebhookController      POST /webhook
│   ├── RevenueController      GET  /revenue
│   ├── GarageController       GET  /garage, POST /garage/reload
│   └── dto/                   Contratos de entrada e saída
├── service/               # Regras de negócio
│   ├── ParkingEventService     ENTRY / PARKED / EXIT
│   ├── PricingService          Preço dinâmico e cálculo do valor
│   ├── GarageService           Configuração e lotação da garagem
│   ├── RevenueService          Apuração do faturamento
│   ├── SimulatorClient         Consome GET /garage do simulador
│   └── GarageBootstrapRunner   Carga inicial na subida da aplicação
├── repository/            # Acesso a dados (Spring Data JPA)
├── model/                 # Entidades: Sector, Spot, ParkingSession
├── exception/             # Exceções de negócio + @RestControllerAdvice
└── config/                # RestClient, OpenAPI, properties do simulador
```

## Como subir

### Com Docker (aplicação + MySQL + simulador)

```bash
docker compose up --build
```

- Aplicação: <http://localhost:3003>
- Swagger: <http://localhost:3003/swagger-ui>
- MySQL: `localhost:3308` (`estapardb` / `appuser` / `apppassword`)
- Simulador: <http://localhost:3000>

### Local (apenas o banco em container)

```bash
docker compose up -d mysql_estapar_app
./mvnw spring-boot:run
```

O simulador é configurável por `SIMULATOR_BASE_URL` (padrão `http://localhost:3000`).

### Testes

```bash
./mvnw test                          # 81 testes
# relatório de cobertura: target/site/jacoco/index.html
```

## Endpoints

### `POST /webhook` — eventos do simulador

Um único payload atende aos três tipos de evento.

**ENTRY**
```json
{ "license_plate": "ZUL0001", "entry_time": "2025-01-01T12:00:00.000Z", "event_type": "ENTRY" }
```

**PARKED**
```json
{ "license_plate": "ZUL0001", "lat": -23.561684, "lng": -46.655981, "event_type": "PARKED" }
```

**EXIT**
```json
{ "license_plate": "ZUL0001", "exit_time": "2025-01-01T12:00:00.000Z", "event_type": "EXIT" }
```

Respostas:

| Status | Quando |
|---|---|
| `200` | Evento aceito e processado |
| `400` | Campos obrigatórios ausentes para o tipo de evento |
| `404` | Veículo sem permanência aberta, ou coordenada sem vaga cadastrada |
| `409` | Garagem lotada, entrada duplicada, vaga já ocupada ou setor fechado |

### `GET /revenue` — faturamento por setor e data

O contrato define corpo JSON no GET; os mesmos campos também são aceitos como query params (mais prático no navegador e no Swagger).

```bash
# corpo JSON (contrato)
curl -X GET http://localhost:3003/revenue \
  -H 'Content-Type: application/json' \
  -d '{ "date": "2025-01-01", "sector": "A" }'

# query params (equivalente)
curl 'http://localhost:3003/revenue?date=2025-01-01&sector=A'
```

```json
{ "amount": 18.00, "currency": "BRL", "timestamp": "2025-01-01T12:00:00.000Z" }
```

### `GET /garage` — configuração armazenada

Devolve setores e vagas no mesmo formato do simulador. `POST /garage/reload` reimporta a configuração — útil quando o simulador ainda não estava disponível na subida da aplicação.

## Regras implementadas

### Ciclo de vida da permanência

`ENTRY` → `PARKED` → `EXIT`, modelado em `ParkingSession` com os status `ENTERED`, `PARKED` e `EXITED`.

- **ENTRY**: recusa entrada duplicada da mesma placa e recusa a entrada com a garagem lotada. Congela o multiplicador de preço dinâmico.
- **PARKED**: resolve a vaga por `lat`/`lng`, marca a vaga como ocupada e vincula o setor à permanência.
- **EXIT**: libera a vaga, calcula o valor e encerra a permanência.

### Preço dinâmico (fixado no momento da entrada)

| Lotação da garagem | Multiplicador |
|---|---|
| < 25% | 0,90 (desconto de 10%) |
| 25% a 49,99% | 1,00 (preço cheio) |
| 50% a 74,99% | 1,10 (acréscimo de 10%) |
| 75% a 100% | 1,25 (acréscimo de 25%) |

### Cálculo do valor

- Primeiros **30 minutos gratuitos**.
- Acima de 30 minutos: tarifa fixa por hora — **inclusive a primeira** — com as horas **arredondadas para cima**.
- `valor = basePrice × multiplicador × horas`, com 2 casas decimais (`HALF_UP`).

Exemplo: entrada com garagem vazia (multiplicador 0,90), `basePrice` 10,00 e 2 h de permanência → `10,00 × 0,90 × 2 = 18,00`.

### Lotação

- **Setor a 100%**: fechado (`sector.closed`), só reabre com a saída de um veículo estacionado.
- **Garagem a 100%**: nenhuma nova entrada é aceita até a liberação de uma vaga.

## Premissas assumidas

O `rule.md` deixa alguns pontos abertos; as decisões tomadas estão registradas aqui.

1. **Faixas do preço dinâmico.** O texto original repete "lotação menor" nas quatro faixas. Foram interpretadas como faixas contíguas e mutuamente exclusivas (`<25%`, `25–50%`, `50–75%`, `75–100%`), na ordem apresentada.

2. **Lotação usada na entrada é a da garagem, não a do setor.** No evento `ENTRY` o setor ainda é desconhecido — ele só é determinado no `PARKED`, pelas coordenadas. Como a regra exige o cálculo "na hora da entrada", a lotação considerada é a da garagem inteira. A tarifa base (`basePrice`) vem do setor resolvido no `PARKED` e é aplicada na saída.

3. **Lotação conta veículos dentro da garagem, não vagas ocupadas.** Um veículo que já enviou `ENTRY` mas ainda não enviou `PARKED` está dentro da garagem e ocupa capacidade. Por isso a taxa de lotação usa permanências abertas ÷ capacidade total. O fechamento de um setor, por sua vez, usa as vagas ocupadas daquele setor.

4. **`EXIT` sem `PARKED` prévio é cobrado como zero.** Sem o `PARKED` não há setor e, portanto, não há tarifa base para aplicar. A permanência é encerrada normalmente (liberando capacidade) e um `WARN` é registrado.

5. **Webhook responde 409 quando a regra recusa o evento.** O contrato prevê `200`, que é o retorno de todo evento aceito. Recusas por lotação, entrada duplicada ou vaga ocupada retornam `409` com corpo de erro, o que torna a regra observável e testável.

6. **Horários são normalizados para UTC.** Os eventos chegam com offset (`...Z`) e são convertidos para UTC antes da persistência. A data de `GET /revenue` é interpretada em UTC.

7. **Faturamento é contabilizado na data de saída**, momento em que a cobrança é efetivada.

8. **Sem capacidade cadastrada, a garagem é considerada lotada.** Se o simulador estiver indisponível na subida, nenhuma entrada é aceita até que a configuração seja importada — evita processar eventos sem setores e vagas conhecidos.

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `sector` | Setor com `base_price`, `max_capacity` e flag `closed` |
| `spot` | Vaga com `id` vindo do simulador, coordenadas e flag `occupied` |
| `parking_session` | Permanência: placa, setor, vaga, horários, multiplicador, valor e status |
