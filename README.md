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
| `atuador` | `ActuatorCooler` / `ActuatorExhaust` | Subscriber | Assina um tópico e reage a comandos diretos recebidos do Broker |
| `cliente` | `ClientTerminalUI` | Publisher + Subscriber | Interface interativa via terminal para monitorar tópicos e enviar mensagens ou comandos |

### Fluxo de Roteamento

O `Broker` suporta dois modos de roteamento:

**Pub/Sub (Telemetria):** A mensagem é difundida para todos os Subscribers inscritos no tópico de destino.

**Ponto a Ponto (Comandos):** Se a mensagem contém um `targetNodeId`, o Broker a entrega diretamente ao nó de destino, ignorando o tópico.

### Sensores e Atuadores Pré-configurados

| Classe | ID | Tópico | Intervalo |
|--------|----|--------|-----------|
| `SensorTemperature` | `sensor-temp-1` | `TEMP` | 2000 ms |
| `SensorHumidity` | `sensor-humidity-1` | `UMI` | 3000 ms |
| `ActuatorCooler` | `cooler-1` | `TEMP` | — |
| `ActuatorExhaust` | `exhaust-1` | `UMI` | — |

---

## Estrutura de Diretórios

```
middleware-iot/
│
├── broker/
│   ├── src/main/java/middleware/
│   │   ├── Broker.java                  # Gerencia conexões, tópicos e roteamento
│   │   ├── MiddlewareHandlerServer.java  # Handler por conexão (thread dedicada)
│   │   └── ServerMain.java              # Ponto de entrada do Broker (porta 8080)
│   ├── Dockerfile
│   └── pom.xml
│
├── sensor/
│   ├── src/main/java/middleware/
│   │   ├── Sensor.java                  # Classe abstrata base para sensores
│   │   ├── SensorTemperature.java       # Publica em "TEMP", simula 25 C a 35 C
│   │   └── SensorHumidity.java          # Publica em "UMI", simula 40% a 60%
│   ├── Dockerfile
│   └── pom.xml
│
├── atuador/
│   ├── src/main/java/middleware/
│   │   ├── Actuator.java                # Classe abstrata base para atuadores
│   │   ├── ActuatorCooler.java          # Assina "TEMP", responde a TURN_ON/TURN_OFF
│   │   └── ActuatorExhaust.java         # Assina "UMI", responde a TURN_ON/TURN_OFF
│   ├── Dockerfile
│   └── pom.xml
│
└── cliente/
    ├── src/main/java/middleware/
    │   ├── Client.java                  # Classe base: gerencia publishers, subscribers e histórico
    │   ├── ClientTerminalUI.java        # Interface interativa via terminal
    │   ├── Middleware.java              # Interface contratual do middleware
    │   ├── Message.java                 # Modelo de mensagem serializada em JSON
    │   ├── CommandType.java             # Enum: TURN_ON, TURN_OFF, STATUS_REQUEST, TELEMETRY, RESPONSE
    │   └── MiddlewareImplementation/
    │       ├── MiddlewareClient.java    # Implementação TCP com filas de envio e recebimento
    │       └── MiddlewareFactory.java   # Factory para instanciar clientes de middleware
    ├── Dockerfile
    └── pom.xml
```

---

## Protocolo de Mensagem

Toda comunicação é serializada em JSON. O campo `connectionType` determina o papel do nó (`PUBLISHER` ou `SUBSCRIBER`). O campo `commandType` classifica o propósito da mensagem.

**Campos disponíveis:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `action` | String | Ação genérica (uso opcional) |
| `connectionType` | String | `"PUBLISHER"` ou `"SUBSCRIBER"` |
| `topic` | String | Tópico da mensagem (ex: `"TEMP"`, `"UMI"`) |
| `nodeId` | String | Identificador do nó remetente |
| `data` | String | Conteúdo da mensagem (ex: `"Temperature: 28C"`) |
| `commandType` | Enum | `TELEMETRY`, `TURN_ON`, `TURN_OFF`, `STATUS_REQUEST`, `RESPONSE` |
| `targetNodeId` | String | ID do nó de destino (para roteamento ponto a ponto) |

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
  "topic": "factory/control",
  "nodeId": "client-gateway",
  "data": "",
  "commandType": "TURN_OFF",
  "targetNodeId": "cooler-1"
}
```

---

## Guia de Execução

> **Ordem de inicialização obrigatória:** O Broker deve estar em execução antes de qualquer Sensor, Atuador ou Cliente ser iniciado.

---

### Opção 1: Maven

Execute os comandos abaixo a partir do diretório raiz de cada módulo.

**Compilação (em cada módulo):**

```bash
mvn clean compile
```

**Execução:**

```bash
# 1. Broker
cd broker
java -cp target/classes middleware.ServerMain

# 2. Atuador Cooler (Subscriber — topico TEMP)
cd atuador
java -cp target/classes middleware.ActuatorCooler

# 3. Atuador Exhaust (Subscriber — topico UMI)
cd atuador
java -cp target/classes middleware.ActuatorExhaust

# 4. Sensor de Temperatura (Publisher — topico TEMP)
cd sensor
java -cp target/classes middleware.SensorTemperature

# 5. Sensor de Umidade (Publisher — topico UMI)
cd sensor
java -cp target/classes middleware.SensorHumidity

# 6. Cliente Terminal (interface interativa)
cd cliente
java -cp target/classes middleware.ClientTerminalUI
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
cd broker && java -cp out middleware.ServerMain

# 2. Atuadores
cd atuador && java -cp out middleware.ActuatorCooler
cd atuador && java -cp out middleware.ActuatorExhaust

# 3. Sensores
cd sensor && java -cp out middleware.SensorTemperature
cd sensor && java -cp out middleware.SensorHumidity

# 4. Cliente Terminal
cd cliente && java -cp out middleware.ClientTerminalUI
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
docker run -d --name cooler  --network rede-iot iot-atuador java middleware.ActuatorCooler
docker run -d --name exhaust --network rede-iot iot-atuador java middleware.ActuatorExhaust

# 3. Sensores (segundo plano)
docker run -d --name sensor-temp --network rede-iot iot-sensor java middleware.SensorTemperature
docker run -d --name sensor-hum  --network rede-iot iot-sensor java middleware.SensorHumidity

# 4. Cliente Terminal (modo interativo)
docker run -it --name cliente --network rede-iot iot-cliente java middleware.ClientTerminalUI
```

> Ao usar Docker, informe `broker` como **host** no cliente terminal (nome do contêiner na rede), no lugar de `localhost`.

---

## Manual do Cliente Terminal

O `ClientTerminalUI` é uma interface de linha de comando que permite interagir com o sistema Pub/Sub em tempo real. Ao iniciar, são solicitadas três informações de configuração:

```
=== Gateway IoT ===
Host: localhost
Porta: 8080
ID do dispositivo: meu-cliente
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

Registra o cliente como Subscriber em um tópico. A partir deste momento, todas as mensagens publicadas nesse tópico serão recebidas e armazenadas no histórico local (máximo de 10 entradas por tópico).

```
Tópico: TEMP
Escutando [TEMP]...
```

Um mesmo cliente pode assinar múltiplos tópicos. Cada assinatura cria uma thread dedicada de leitura.

---

### Opção 2 — Criar publicador

Registra o cliente como Publisher em um tópico, estabelecendo um canal de envio. Este passo é obrigatório antes de usar as opções 3 e 4 para aquele tópico.

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

Envia uma mensagem diretamente a um nó específico pelo seu ID, utilizando o campo `targetNodeId`. O Broker entrega a mensagem ao nó de destino sem difundi-la aos demais Subscribers.

```
Tópico da rede: TEMP
ID do Atuador de destino: cooler-1
Comandos: 1-TURN_ON | 2-TURN_OFF | 3-STATUS_REQUEST
Escolha: 2
Dados adicionais: desligamento manual
Comando despachado.
```

Pré-requisito: o canal para o tópico informado deve ter sido criado com a opção 2. O nó de destino deve estar conectado ao Broker no momento do envio.

**Comandos disponíveis:**

| Escolha | CommandType | Efeito no Atuador |
|---------|-------------|-------------------|
| 1 | `TURN_ON` | Define `isOn = true` e imprime status ON |
| 2 | `TURN_OFF` | Define `isOn = false` e imprime status OFF |
| 3 | `STATUS_REQUEST` | Sem efeito definido na implementação atual |

---

### Opção 5 — Mostrar Histórico do Tópico

Exibe as últimas mensagens recebidas em um tópico assinado. O histórico mantém até 10 entradas por tópico, removendo as mais antigas quando o limite é atingido.

```
Qual tópico deseja consultar o histórico? TEMP
--------------------------------------------------
| TOPIC: TEMP                                    |
--------------------------------------------------
| INDEX | DATA                                   |
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

O exemplo abaixo demonstra um cliente que monitora o tópico `TEMP` e envia um comando de desligamento ao cooler:

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
| INDEX | DATA                                   |
--------------------------------------------------
|     1 | Temperature: 25C                       |
|     2 | Temperature: 26C                       |
--------------------------------------------------

Opção: 4
Tópico da rede: TEMP
ID do Atuador de destino: cooler-1
Comandos: 1-TURN_ON | 2-TURN_OFF | 3-STATUS_REQUEST
Escolha: 2
Dados adicionais:
Comando despachado.
```

---

## Comportamento Automático do Broker

O `MiddlewareHandlerServer` implementa uma camada de automação que monitora o tópico `factory/telemetry` e aciona comandos automaticamente com base em limites críticos:

| Condição | Ação Automática |
|----------|----------------|
| `Temperature >= 35` | Envia `TURN_OFF` para `cooler-1` via tópico `factory/control` |
| `Humidity >= 60` | Envia `TURN_OFF` para `exhaust-1` via tópico `factory/control` |

Este comportamento ocorre independentemente de qualquer cliente conectado e é executado diretamente na thread de cada handler do servidor.

---

## Tecnologias

| Tecnologia | Uso |
|------------|-----|
| Java SE | Implementação orientada a objetos de todos os módulos |
| Sockets TCP/IP | Transporte confiável para comunicação entre os nós |
| Multithreading (`Thread`, `BlockingQueue`) | Atendimento concorrente no Broker; filas de envio e recebimento no cliente |
| Jackson (`ObjectMapper`) | Serialização e desserialização de mensagens em JSON |
| Maven | Gerenciamento de dependências e build por módulo |
| Docker | Containerização e isolamento de ambiente por módulo |
