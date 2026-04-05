# Middleware IoT — Publish/Subscribe

Sistema de middleware para Internet das Coisas (IoT) baseado no padrão arquitetural **Publish/Subscribe**, implementado em Java com comunicação via **Sockets TCP** e serialização de dados em **JSON**. O sistema é composto por quatro módulos independentes, cada um com seu próprio `pom.xml` e `Dockerfile`.

---

## Sumário

- [Arquitetura](#arquitetura)
- [Estrutura de Diretórios](#estrutura-de-diretórios)
- [Protocolo de Mensagem](#protocolo-de-mensagem)
- [Guia de Execução](#guia-de-execução)
  - [Maven](#opção-1-maven)
  - [javac](#opção-2-compilador-java-nativo-javac)
  - [Docker](#opção-3-docker)
- [Manual do Cliente Terminal](#manual-do-cliente-terminal)
- [Comportamento Automático do Broker](#comportamento-automático-do-broker)
- [Tecnologias](#tecnologias)

---

## Arquitetura

O sistema é organizado em quatro módulos com responsabilidades bem definidas:

| Módulo | Classe Principal | Papel | Descrição |
|--------|-----------------|-------|-----------|
| `broker` | `ServerMain` | Coordenador | Aceita conexões TCP, gerencia tópicos e roteia mensagens entre os nós conectados |
| `sensor` | `SensorTemperature` / `SensorHumidity` | Publisher | Gera dados periódicos de telemetria e os publica em um tópico via Broker |
| `atuador` | `ActuatorCooler` / `ActuatorExhaust` | Publisher + Subscriber | Assina um tópico, reage a comandos diretos e envia ACKs de confirmação ao remetente |
| `cliente` | `ClientTerminalUI` | Publisher + Subscriber | Interface interativa via terminal para monitorar tópicos, enviar mensagens ou comandos e aguardar respostas |

### Fluxo de Roteamento

O `Broker` suporta dois modos de roteamento:

**Pub/Sub (Telemetria):** A mensagem é difundida para todos os Subscribers inscritos no tópico de destino. Utilizado por sensores para envio contínuo de dados.

**Ponto a Ponto (Comandos e Respostas):** Se a mensagem contém um `targetNodeId`, o Broker localiza o nó de destino — primeiro entre os Subscribers do tópico, depois pelo índice global de IDs — e entrega a mensagem diretamente, sem difusão.

### Mecanismo de Confirmação (ACK)

Ao receber um comando (`TURN_ON`, `TURN_OFF`, `STATUS_REQUEST`), o Atuador responde automaticamente ao remetente com uma mensagem do tipo `RESPONSE`:

| Comando recebido | Resposta enviada |
|------------------|-----------------|
| `TURN_ON` | `"ACK_TURN_ON"` |
| `TURN_OFF` | `"ACK_TURN_OFF"` |
| `STATUS_REQUEST` | `"STATUS: ON"` ou `"STATUS: OFF"` |

O cliente pode aguardar essa resposta com o método `waitForResponse(expectedNodeId, timeoutMillis)`, que bloqueia a thread até que o ACK do nó esperado chegue ou o tempo limite seja atingido.

### Sensores e Atuadores Pré-configurados

| Classe | ID | Tópico | Comportamento |
|--------|----|--------|---------------|
| `SensorTemperature` | `sensor-temp-1` | `TEMP` | Publica a cada 2000 ms, simula 25 C a 35 C |
| `SensorHumidity` | `sensor-humidity-1` | `UMI` | Publica a cada 3000 ms, simula 40% a 60% |
| `ActuatorCooler` | `ACTUATOR-COOLER` | `TEMP` | Assina e publica em `TEMP`; responde a comandos com ACK |
| `ActuatorExhaust` | `ACTUATOR-EXHAUST` | `UMI` | Assina e publica em `UMI`; responde a comandos com ACK |

---

## Estrutura de Diretórios

Cada módulo replica a mesma divisão interna em três pacotes, separando implementação, modelos e enumerações:

```
middleware-iot/
│
├── broker/
│   ├── src/main/java/
│   │   ├── implementation/
│   │   │   ├── Broker.java                  # Gerencia conexões, tópicos e roteamento
│   │   │   ├── MiddlewareHandlerServer.java  # Handler por conexão TCP (thread dedicada)
│   │   │   └── ServerMain.java              # Ponto de entrada do Broker (porta 8080)
│   │   ├── data_type/
│   │   │   └── Message.java                 # Modelo de mensagem serializada em JSON
│   │   └── enums/
│   │       ├── CommandType.java             # TURN_ON, TURN_OFF, STATUS_REQUEST, TELEMETRY, RESPONSE
│   │       └── TopicType.java               # TEMP, UMI
│   ├── Dockerfile
│   └── pom.xml
│
├── sensor/
│   ├── src/main/java/
│   │   ├── implementation/
│   │   │   ├── Sensor.java                  # Classe abstrata base para sensores
│   │   │   ├── SensorTemperature.java       # Publica em TEMP a cada 2000 ms
│   │   │   └── SensorHumidity.java          # Publica em UMI a cada 3000 ms
│   │   ├── data_type/
│   │   │   └── Message.java
│   │   └── enums/
│   │       ├── CommandType.java
│   │       └── TopicType.java
│   ├── Dockerfile
│   └── pom.xml
│
├── atuador/
│   ├── src/main/java/
│   │   ├── implementation/
│   │   │   ├── Actuator.java                # Classe abstrata base; lida com comandos e envia ACKs
│   │   │   ├── ActuatorCooler.java          # ID: ACTUATOR-COOLER, tópico: TEMP
│   │   │   └── ActuatorExhaust.java         # ID: ACTUATOR-EXHAUST, tópico: UMI
│   │   ├── data_type/
│   │   │   └── Message.java
│   │   └── enums/
│   │       ├── CommandType.java
│   │       └── TopicType.java
│   ├── Dockerfile
│   └── pom.xml
│
└── cliente/
    ├── src/main/java/
    │   ├── implementation/
    │   │   ├── Client.java                  # Classe base: publishers, subscribers, histórico e ACK
    │   │   ├── ClientTerminalUI.java        # Interface interativa via terminal
    │   │   ├── Middleware.java              # Interface contratual do middleware
    │   │   ├── MiddlewareClient.java        # Implementação TCP com filas de envio e recebimento
    │   │   └── MiddlewareFactory.java       # Factory para instanciar clientes de middleware
    │   ├── data_type/
    │   │   └── Message.java
    │   └── enums/
    │       ├── CommandType.java
    │       └── TopicType.java
    ├── Dockerfile
    └── pom.xml
```

---

## Protocolo de Mensagem

Toda comunicação é serializada em JSON. O campo `topic` utiliza o enum `TopicType` (`TEMP` ou `UMI`). O campo `commandType` classifica o propósito da mensagem.

**Campos disponíveis na classe `Message`:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `connectionType` | String | `"PUBLISHER"` ou `"SUBSCRIBER"` |
| `topic` | `TopicType` | Tópico da mensagem: `TEMP` ou `UMI` |
| `nodeId` | String | Identificador do nó remetente |
| `data` | String | Conteúdo da mensagem (ex: `"Temperature: 28C"`) |
| `commandType` | `CommandType` | `TELEMETRY`, `TURN_ON`, `TURN_OFF`, `STATUS_REQUEST`, `RESPONSE` |
| `targetNodeId` | String | ID do nó de destino (roteamento ponto a ponto) |

**Exemplo — Telemetria (Pub/Sub):**

```json
{
  "connectionType": "PUBLISHER",
  "topic": "TEMP",
  "nodeId": "sensor-temp-1",
  "data": "Temperature: 28C",
  "commandType": "TELEMETRY"
}
```

**Exemplo — Comando direto (Ponto a Ponto):**

```json
{
  "connectionType": "PUBLISHER",
  "topic": "TEMP",
  "nodeId": "operador-1",
  "data": "",
  "commandType": "TURN_OFF",
  "targetNodeId": "ACTUATOR-COOLER"
}
```

**Exemplo — Resposta (ACK):**

```json
{
  "connectionType": "PUBLISHER",
  "topic": "TEMP",
  "nodeId": "ACTUATOR-COOLER",
  "data": "ACK_TURN_OFF",
  "commandType": "RESPONSE",
  "targetNodeId": "operador-1"
}
```

---

## Guia de Execução

> **Ordem de inicialização obrigatória:** O Broker deve estar em execução antes de qualquer Sensor, Atuador ou Cliente ser iniciado. Atuadores devem ser iniciados antes dos Sensores quando se deseja que os ACKs automáticos funcionem desde o início.

---

### Opção 1: Maven

Execute os comandos abaixo a partir do diretório raiz de cada módulo.

**Compilação:**

```bash
mvn clean compile
```

**Execução:**

```bash
# 1. Broker
cd broker
java -cp target/classes implementation.ServerMain

# 2. Atuador Cooler (Subscriber + Publisher — topico TEMP)
cd atuador
java -cp target/classes implementation.ActuatorCooler

# 3. Atuador Exhaust (Subscriber + Publisher — topico UMI)
cd atuador
java -cp target/classes implementation.ActuatorExhaust

# 4. Sensor de Temperatura (Publisher — topico TEMP)
cd sensor
java -cp target/classes implementation.SensorTemperature

# 5. Sensor de Umidade (Publisher — topico UMI)
cd sensor
java -cp target/classes implementation.SensorHumidity

# 6. Cliente Terminal (interface interativa)
cd cliente
java -cp target/classes implementation.ClientTerminalUI
```

---

### Opção 2: Compilador Java Nativo (`javac`)

**Compilação (em cada módulo):**

```bash
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
```

**Execução:**

```bash
# 1. Broker
cd broker && java -cp out implementation.ServerMain

# 2. Atuadores
cd atuador && java -cp out implementation.ActuatorCooler
cd atuador && java -cp out implementation.ActuatorExhaust

# 3. Sensores
cd sensor && java -cp out implementation.SensorTemperature
cd sensor && java -cp out implementation.SensorHumidity

# 4. Cliente Terminal
cd cliente && java -cp out implementation.ClientTerminalUI
```

---

### Opção 3: Docker

Cada módulo possui seu próprio `Dockerfile`. Todos os contêineres devem compartilhar a mesma rede virtual para que a comunicação via Socket funcione.

**Construção das imagens:**

```bash
docker build -t iot-broker   ./broker
docker build -t iot-sensor   ./sensor
docker build -t iot-atuador  ./atuador
docker build -t iot-cliente  ./cliente
```

**Criação da rede virtual:**

```bash
docker network create rede-iot
```

**Execução:**

```bash
# 1. Broker (segundo plano)
docker run -d --name broker --network rede-iot iot-broker

# 2. Atuadores (segundo plano)
docker run -d --name cooler  --network rede-iot iot-atuador java implementation.ActuatorCooler
docker run -d --name exhaust --network rede-iot iot-atuador java implementation.ActuatorExhaust

# 3. Sensores (segundo plano)
docker run -d --name sensor-temp --network rede-iot iot-sensor java implementation.SensorTemperature
docker run -d --name sensor-hum  --network rede-iot iot-sensor java implementation.SensorHumidity

# 4. Cliente Terminal (modo interativo)
docker run -it --name cliente --network rede-iot iot-cliente java implementation.ClientTerminalUI
```

> Ao usar Docker, informe `broker` como **host** no cliente terminal (nome do contêiner na rede), no lugar de `localhost`.

---

## Manual do Cliente Terminal

O `ClientTerminalUI` é uma interface de linha de comando que permite interagir com o sistema Pub/Sub em tempo real. Ao iniciar, são solicitadas três informações de configuração:

```
=== Gateway IoT ===
Host: localhost
Porta: 8080
ID do dispositivo: operador-1
```

Após a configuração, o menu principal é exibido a cada interação.

---

### Menu Principal

```
--- MENU ---
1. Assinar tópico (Receber)
2. Criar publicador (Enviar)
3. Enviar Telemetria (Broadcast)
4. Enviar Comando (Ponto-a-Ponto)
5. Mostrar Histórico do Tópico
6. Sair
```

---

### Opção 1 — Assinar tópico

Registra o cliente como Subscriber em um tópico. A partir deste momento, todas as mensagens do tipo `TELEMETRY` publicadas nesse tópico serão recebidas e armazenadas no histórico local. Mensagens do tipo `RESPONSE` são interceptadas pelo mecanismo de ACK e não entram no histórico. O histórico mantém as últimas 10 entradas por tópico.

```
Tópico: TEMP
Escutando [TEMP]...
```

Um mesmo cliente pode assinar múltiplos tópicos. Cada assinatura cria uma thread dedicada de leitura.

Os tópicos disponíveis são definidos pelo enum `TopicType`:

| Valor | Descrição |
|-------|-----------|
| `TEMP` | Dados de temperatura |
| `UMI` | Dados de umidade |

---

### Opção 2 — Criar publicador

Registra o cliente como Publisher em um tópico, estabelecendo um canal de envio. Este passo é obrigatório antes de usar as opções 3 e 4 para aquele tópico. Um publisher também inicia uma thread de escuta, permitindo ao cliente receber ACKs de volta pelos canais que abriu.

```
Tópico: TEMP
Canal criado para [TEMP].
```

---

### Opção 3 — Enviar Telemetria (Broadcast)

Envia uma mensagem do tipo `TELEMETRY` para todos os Subscribers inscritos no tópico informado. O campo `targetNodeId` não é preenchido, portanto o Broker realiza o roteamento Pub/Sub padrão.

```
Tópico de envio: TEMP
Mensagem: Temperature: 30C
```

Pré-requisito: o canal para o tópico informado deve ter sido criado com a opção 2.

---

### Opção 4 — Enviar Comando (Ponto-a-Ponto)

Envia uma mensagem diretamente a um nó específico pelo seu ID, utilizando o campo `targetNodeId`. O Broker entrega a mensagem ao nó de destino sem difundi-la aos demais Subscribers. Após o envio, o cliente aguarda automaticamente o ACK do atuador antes de retornar ao menu.

```
Tópico da rede: TEMP
ID do Atuador de destino: ACTUATOR-COOLER
Comandos: 1-TURN_ON | 2-TURN_OFF | 3-STATUS_REQUEST
Escolha: 3
Dados adicionais:
Comando despachado.
[Aguardando resposta de ACTUATOR-COOLER...]
[ACTUATOR-COOLER] Resposta: STATUS: ON
```

Pré-requisito: o canal para o tópico informado deve ter sido criado com a opção 2. O nó de destino deve estar conectado ao Broker no momento do envio.

**Comandos disponíveis:**

| Escolha | CommandType | Efeito no Atuador | ACK retornado |
|---------|-------------|-------------------|---------------|
| 1 | `TURN_ON` | Define estado como ON | `"ACK_TURN_ON"` |
| 2 | `TURN_OFF` | Define estado como OFF | `"ACK_TURN_OFF"` |
| 3 | `STATUS_REQUEST` | Sem alteração de estado | `"STATUS: ON"` ou `"STATUS: OFF"` |

---

### Opção 5 — Mostrar Histórico do Tópico

Exibe as últimas mensagens de telemetria recebidas em um tópico assinado. O histórico mantém até 10 entradas, removendo as mais antigas quando o limite é atingido. Apenas mensagens do tipo `TELEMETRY` são armazenadas; ACKs e comandos não aparecem aqui.

```
Qual tópico deseja consultar o histórico? TEMP
--------------------------------------------------
| TOPIC: TEMP                                    |
--------------------------------------------------
|     1 | Temperature: 25C                       |
|     2 | Temperature: 26C                       |
|     3 | Temperature: 27C                       |
--------------------------------------------------
```

Caso o tópico não tenha sido assinado ou ainda não tenha recebido mensagens, a tabela exibirá `NO DATA`.

---

### Opção 6 — Sair

Encerra o processo do cliente.

---

### Exemplo de sessão completa

O exemplo abaixo demonstra um cliente que monitora o tópico `TEMP`, consulta o histórico e envia um comando de desligamento ao cooler, aguardando a confirmação:

```
=== Gateway IoT ===
Host: localhost
Porta: 8080
ID do dispositivo: operador-1

Opção: 1
Tópico: TEMP
Escutando [TEMP]...

Opção: 2
Tópico: TEMP
Canal criado para [TEMP].

Opção: 5
Qual tópico deseja consultar o histórico? TEMP
--------------------------------------------------
| TOPIC: TEMP                                    |
--------------------------------------------------
|     1 | Temperature: 25C                       |
|     2 | Temperature: 26C                       |
--------------------------------------------------

Opção: 4
Tópico da rede: TEMP
ID do Atuador de destino: ACTUATOR-COOLER
Comandos: 1-TURN_ON | 2-TURN_OFF | 3-STATUS_REQUEST
Escolha: 2
Dados adicionais:
Comando despachado.
[Aguardando resposta de ACTUATOR-COOLER...]
[ACTUATOR-COOLER] Resposta: ACK_TURN_OFF
```

---

## Comportamento Automático do Broker

O `MiddlewareHandlerServer` monitora cada mensagem recebida e dispara comandos automaticamente quando os valores de telemetria ultrapassam limites críticos. Esse comportamento ocorre independentemente de qualquer cliente conectado.

| Tópico | Condição | Ação Automática | Alvo |
|--------|----------|----------------|------|
| `TEMP` | `Temperature >= 40` | Envia `TURN_OFF` | `ACTUATOR-COOLER` |
| `UMI` | `Humidity >= 80` | Envia `TURN_OFF` | `ACTUATOR-EXHAUST` |

O comando é processado pelo próprio `Broker` via `processMessage`, com `nodeId` definido como `SERVER_HANDLER` e `data` definido como `"CRITICAL_LIMIT_REACHED"`.

---

## Tecnologias

| Tecnologia | Uso |
|------------|-----|
| Java SE | Implementação orientada a objetos de todos os módulos |
| Sockets TCP/IP | Transporte confiável para comunicação entre os nós |
| Multithreading (`Thread`, `BlockingQueue`) | Atendimento concorrente no Broker; filas de envio e recebimento no cliente; mecanismo de ACK com `wait`/`notifyAll` |
| Jackson (`ObjectMapper`) | Serialização e desserialização de mensagens em JSON |
| Maven | Gerenciamento de dependências e build por módulo |
| Docker | Containerização e isolamento de ambiente por módulo |
