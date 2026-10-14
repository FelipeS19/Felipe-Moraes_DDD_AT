# Freela Marketplace

Projeto de referência para um marketplace de contratação de freelancers construído com arquitetura de microsserviços em Java e Spring.

A aplicação representa um cenário em que clientes contratam freelancers para a execução de trabalhos. O núcleo do sistema é o gerenciamento dos contratos firmados entre as partes. A partir desse domínio, outros serviços mantêm informações relacionadas a notificações, reputação e auditoria.

## Visão geral

O sistema é composto por seis aplicações Spring Boot:

- `eureka-server`: registro e descoberta dos serviços.
- `api-gateway`: ponto de entrada HTTP da aplicação.
- `contrato-service`: gerenciamento dos contratos entre clientes e freelancers.
- `notificacao-service`: armazenamento das notificações relacionadas aos contratos.
- `reputacao-service`: manutenção de informações agregadas sobre os freelancers.
- `auditoria-service`: registro de eventos relevantes do sistema.

A infraestrutura local utiliza PostgreSQL e Apache Kafka.

```text
                         +-------------------+
                         |      Cliente      |
                         +---------+---------+
                                   |
                                   | HTTP
                                   v
                         +-------------------+
                         |    API Gateway    |
                         |       :8080       |
                         +---------+---------+
                                   |
                     Service Discovery / Eureka
                                   |
                +------------------+------------------+
                |                  |                  |
                v                  v                  v
       +----------------+  +----------------+  +----------------+
       | contrato       |  | notificacao    |  | reputacao      |
       | service :8081  |  | service :8082  |  | service :8083  |
       +----------------+  +----------------+  +----------------+
                |
                |                       +----------------+
                +---------------------->| auditoria      |
                                        | service :8084  |
                                        +----------------+

                          +-------------------+
                          |       Kafka       |
                          |       :9092       |
                          +-------------------+

                          +-------------------+
                          |    PostgreSQL     |
                          |       :5432       |
                          +-------------------+
```

## Domínio

O domínio principal está no `contrato-service`.

Um contrato representa o vínculo entre um cliente e um freelancer para a execução de um trabalho. Cada contrato possui:

- identificador;
- cliente;
- freelancer;
- título do trabalho;
- valor;
- status;
- data de criação.

Os estados disponíveis são:

```text
ATIVO
ENTREGA_REGISTRADA
CONCLUIDO
CANCELADO
```

O fluxo de negócio previsto pelo modelo é:

```text
ATIVO
  |
  v
ENTREGA_REGISTRADA
  |
  v
CONCLUIDO
```

Um contrato ativo também pode ser cancelado.

O `contrato-service` utiliza uma organização inspirada em Domain-Driven Design, separando domínio, aplicação e infraestrutura.

```text
contrato-service
└── src/main/java/br/com/freela/contrato
    ├── application
    ├── domain
    │   ├── event
    │   ├── model
    │   ├── repository
    │   └── shared
    └── infrastructure
        ├── persistence
        └── web
```

O Aggregate `Contrato` concentra as regras relacionadas às mudanças de estado e produz eventos de domínio. Atualmente existe o evento `ContratoCriado`, que contém as principais informações do contrato no momento da criação.

## Serviços

### contrato-service

Responsável pelo ciclo de vida dos contratos.

Porta:

```text
8081
```

Banco:

```text
contrato_db
```

Principais recursos HTTP:

```text
POST /api/contratos
GET  /api/contratos
GET  /api/contratos/{id}
```

Exemplo de criação de contrato:

```json
{
  "clienteId": "11111111-1111-1111-1111-111111111111",
  "freelancerId": "22222222-2222-2222-2222-222222222222",
  "titulo": "Construção de API de pagamentos",
  "valor": 3500.00
}
```

### notificacao-service

Mantém notificações relacionadas aos acontecimentos do marketplace.

Porta:

```text
8082
```

Banco:

```text
notificacao_db
```

As notificações armazenam informações como contrato, destinatário, tipo, mensagem e momento de criação.

### reputacao-service

Mantém informações agregadas sobre a atividade dos freelancers.

Porta:

```text
8083
```

Banco:

```text
reputacao_db
```

Para cada freelancer são mantidos dados como quantidade de contratos concluídos e valor total dos contratos registrados.

Endpoint disponível para consulta:

```text
GET /api/reputacoes
```

### auditoria-service

Responsável pelo armazenamento de registros associados aos eventos do sistema.

Porta:

```text
8084
```

Banco:

```text
auditoria_db
```

Cada registro de auditoria pode armazenar:

- `eventId`;
- `aggregateId`;
- tipo do evento;
- `correlationId`;
- payload original;
- horário de recebimento.

Endpoint disponível para consulta:

```text
GET /api/auditoria
```

## API Gateway

O `api-gateway` é o ponto de entrada HTTP para os microsserviços.

Porta:

```text
8080
```

As rotas configuradas são:

| Caminho | Serviço |
|---|---|
| `/api/contratos/**` | `contrato-service` |
| `/api/notificacoes/**` | `notificacao-service` |
| `/api/reputacoes/**` | `reputacao-service` |
| `/api/auditoria/**` | `auditoria-service` |

O Gateway utiliza Eureka para localizar as instâncias dos serviços.

Também existe suporte ao header:

```text
X-Correlation-Id
```

Quando o header não é enviado pelo cliente, o Gateway gera automaticamente um UUID e o encaminha para o serviço de destino.

## Eureka Server

O Eureka Server mantém o registro das aplicações disponíveis no ambiente.

Porta:

```text
8761
```

Interface web:

```text
http://localhost:8761
```

Os microsserviços utilizam, por padrão:

```text
http://localhost:8761/eureka/
```

como endereço do service registry.

## PostgreSQL

O ambiente utiliza uma única instância PostgreSQL com bancos separados para cada serviço.

```text
Host:     localhost
Porta:    5432
Usuário:  freela
Senha:    freela
```

Bancos criados durante a inicialização:

```text
contrato_db
notificacao_db
reputacao_db
auditoria_db
```

O script de criação dos bancos está em:

```text
infra/postgres/init-databases.sql
```

Os serviços utilizam Hibernate com `ddl-auto: update` para criação e atualização das tabelas locais.

## Apache Kafka

O Apache Kafka é executado em modo KRaft, sem ZooKeeper.

Para aplicações executadas diretamente na máquina:

```text
localhost:9092
```

Para aplicações executadas dentro da rede Docker:

```text
kafka:19092
```

O broker possui listeners separados para comunicação interna e externa.

O ambiente também inclui o Kafka UI.

```text
http://localhost:8090
```

## Logs

Todos os serviços utilizam logs em nível `INFO` com um formato comum contendo data, nível, nome da aplicação, thread, logger e mensagem.

Exemplo:

```text
2026-09-14 14:42:18.431 INFO service=contrato-service thread=http-nio-8081-exec-1 logger=b.c.f.c.a.ContratoApplicationService - contrato.criacao.inicio clienteId=... freelancerId=...
```

O código registra pontos importantes do fluxo, incluindo:

```text
gateway.request.inicio
gateway.request.fim
http.contrato.criar
contrato.criacao.inicio
contrato.dominio.criado
contrato.persistence.save.inicio
contrato.persistence.save.sucesso
contrato.evento.pendente
contrato.criacao.sucesso
reputacao.atualizacao.inicio
reputacao.atualizacao.sucesso
auditoria.registro.inicio
auditoria.registro.sucesso
```

A presença do `correlationId` nas chamadas HTTP permite relacionar logs produzidos durante uma mesma requisição.

## Infraestrutura local

Os serviços de infraestrutura estão definidos em:

```text
infra/docker-compose.yml
```

Para iniciar o ambiente:

```bash
cd infra
docker compose up -d
```

Para verificar os containers:

```bash
docker compose ps
```

Para encerrar:

```bash
docker compose down
```

Os dados do PostgreSQL são mantidos em volume Docker.

Para remover também os dados persistidos:

```bash
docker compose down -v
```

## Execução das aplicações

A partir da raiz do projeto, cada módulo pode ser iniciado separadamente com Maven.

Eureka Server:

```bash
mvn -pl eureka-server spring-boot:run
```

API Gateway:

```bash
mvn -pl api-gateway spring-boot:run
```

Contrato Service:

```bash
mvn -pl contrato-service spring-boot:run
```

Notificação Service:

```bash
mvn -pl notificacao-service spring-boot:run
```

Reputação Service:

```bash
mvn -pl reputacao-service spring-boot:run
```

Auditoria Service:

```bash
mvn -pl auditoria-service spring-boot:run
```

## Portas e Serviços

| Componente | Porta / URL | Descrição |
|---|---|---|
| API Gateway | `http://localhost:8080` | Ponto de entrada HTTP unificado |
| contrato-service | `http://localhost:8081` | Ciclo de vida de contratos |
| notificacao-service | `http://localhost:8082` | Registro de notificações |
| reputacao-service | `http://localhost:8083` | Reputação agregada de freelancers |
| auditoria-service | `http://localhost:8084` | Trilha de auditoria de eventos |
| Eureka Server | `http://localhost:8761` | Service Registry & Discovery |
| Kafka (Broker KRaft) | `localhost:9092` | Mensageria distribuída |
| Kafka UI | `http://localhost:8090` | Interface web para tópicos e mensagens |
| PostgreSQL | `localhost:5432` | Bancos separados por serviço |
| Zipkin | `http://localhost:9411` | Rastreamento distribuído de traces |
| Elasticsearch | `http://localhost:9200` | Motor de busca e indexação de logs |
| Logstash | `localhost:5000` (TCP) | Pipeline de ingestão e parsing de logs |
| Kibana | `http://localhost:5601` | Interface web para pesquisa centralizada de logs |

## Teste do Fluxo Completo de Ciclo de Vida

Com a infraestrutura (`docker compose up -d`) e as aplicações em execução:

### 1. Criar Contrato (Status: ATIVO)
```bash
curl -i -X POST http://localhost:8080/api/contratos \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: teste-fluxo-001' \
  -d '{
    "clienteId": "11111111-1111-1111-1111-111111111111",
    "freelancerId": "22222222-2222-2222-2222-222222222222",
    "titulo": "Construção de API de pagamentos",
    "valor": 3500.00
  }'
```
*Gera evento `ContratoCriado` no Kafka via Transactional Outbox.*

### 2. Registrar Entrega do Trabalho (Status: ENTREGA_REGISTRADA)
```bash
curl -i -X PATCH http://localhost:8080/api/contratos/{id}/entrega \
  -H 'X-Correlation-Id: teste-fluxo-002'
```
*Gera evento `ContratoEntregaRegistrada` no Kafka.*

### 3. Concluir Contrato (Status: CONCLUIDO)
```bash
curl -i -X PATCH http://localhost:8080/api/contratos/{id}/concluir \
  -H 'X-Correlation-Id: teste-fluxo-003'
```
*Gera evento `ContratoConcluido` no Kafka, atualizando reputação.*

### 4. Consultar os Serviços Consumidores

```bash
# Consultar notificações geradas:
curl http://localhost:8080/api/notificacoes

# Consultar reputação agregada do freelancer:
curl http://localhost:8080/api/reputacoes

# Consultar trilha de auditoria:
curl http://localhost:8080/api/auditoria
```

## Documentação Complementar

* [ARQUITETURA.md](ARQUITETURA.md): Decisões de arquitetura, Outbox, Concorrência, Idempotência e Observabilidade.
* [CONTRATOS-EVENTOS.md](CONTRATOS-EVENTOS.md): Especificação formal dos contratos de mensagens e payloads JSON.
* [EVIDENCIAS.md](EVIDENCIAS.md): Comprovações e evidências detalhadas de execução para avaliação.

## Tecnologias

```text
Java 21
Spring Boot 4.1
Spring Cloud
Spring Cloud Gateway
Netflix Eureka
Spring Data JPA
PostgreSQL 16
Apache Kafka 4 (KRaft)
ELK Stack (Elasticsearch, Logstash, Kibana)
OpenZipkin & Micrometer Tracing (Brave)
Docker Compose
Maven
```
