# 📡 Middleware IoT — Publish/Subscribe

> Sistema de middleware para Internet das Coisas baseado no padrão arquitetural **Publish/Subscribe**, com comunicação assíncrona via **Sockets TCP** e serialização de dados em **JSON**.

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Arquitetura](#-arquitetura-e-estrutura-do-projeto)
- [Guia de Execução](#-guia-de-execução)
  - [Maven](#opção-1-maven-recomendado)
  - [javac](#opção-2-compilador-java-nativo-javac)
  - [Docker](#opção-3-docker)
- [Protocolo de Mensagem](#-especificação-do-protocolo-de-mensagem)
- [Tecnologias](#-tecnologias-aplicadas)

---

## 💡 Sobre o Projeto

Este repositório contém a implementação de um sistema de **Middleware para Internet das Coisas (IoT)** construído inteiramente em Java. O sistema foca no **desacoplamento de dispositivos** de hardware e software por meio da troca assíncrona de mensagens, utilizando o padrão Pub/Sub como espinha dorsal da arquitetura.

---

## 🏗 Arquitetura e Estrutura do Projeto

O sistema é **descentralizado nas pontas**, mas coordenado por um elemento centralizador de mensagens. O código-fonte está organizado em quatro pacotes principais:

| Pacote | Papel | Descrição |
|--------|-------|-----------|
| `broker` | Coordenador | Recebe publicações, gerencia tópicos ativos e encaminha mensagens aos assinantes correspondentes |
| `sensor` | Publisher | Coleta dados ambientais ou de simulação e os envia ao Broker |
| `atuador` | Subscriber | Inscreve-se em tópicos de interesse e reage às mensagens recebidas |
| `cliente` | Base | Encapsula a lógica de conexão TCP e abstrações comuns usadas por sensores e atuadores |

### Estrutura Interna dos Pacotes

Cada pacote é subdividido para manter a organização do domínio:

```
<pacote>/
├── implementation/   # Lógica de execução, ServerSocket/Socket e controle de Threads
├── data_type/        # Estruturas de dados, mapeamento de payloads e classe modelo Message
└── enums/            # Tipos enumerados de configuração (operações de rede, categorias de sensores)
```

---

## 🚀 Guia de Execução

> ⚠️ **Atenção:** Em qualquer método de execução, a **ordem de inicialização é estrita**. O **Broker deve estar online** e aguardando conexões antes que qualquer Atuador ou Sensor seja iniciado.

---

### Opção 1: Maven *(Recomendado)*

**Compilação:**

```bash
mvn clean compile
```

**Execução:**

```bash
# 1. Broker
java -cp target/classes br.uefs.middleware.broker.implementation.Main

# 2. Atuador (Subscriber)
java -cp target/classes br.uefs.middleware.atuador.implementation.Main

# 3. Sensor (Publisher)
java -cp target/classes br.uefs.middleware.sensor.implementation.Main
```

---

### Opção 2: Compilador Java Nativo (`javac`)

Ideal para ambientes Linux/WSL sem gerenciadores de dependências.

**Compilação:**

```bash
# Cria o diretório de saída e compila recursivamente os arquivos .java
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
```

**Execução:**

```bash
# 1. Broker
java -cp out br.uefs.middleware.broker.implementation.Main

# 2. Atuador (Subscriber)
java -cp out br.uefs.middleware.atuador.implementation.Main

# 3. Sensor (Publisher)
java -cp out br.uefs.middleware.sensor.implementation.Main
```

---

### Opção 3: Docker

Garante isolamento de ambiente através de contêineres. Requer um `Dockerfile` configurado na raiz do projeto.

**Construção da imagem:**

```bash
docker build -t iot-middleware .
```

**Configuração de rede:**

Crie uma rede virtual para que os contêineres possam se comunicar via Sockets:

```bash
docker network create rede-iot
```

**Execução:**

```bash
# 1. Broker (roda em segundo plano na rede criada)
docker run -d --name broker --network rede-iot iot-middleware \
  java br.uefs.middleware.broker.implementation.Main

# 2. Atuador (Subscriber)
docker run -d --name atuador --network rede-iot iot-middleware \
  java br.uefs.middleware.atuador.implementation.Main

# 3. Sensor (Publisher)
docker run -d --name sensor --network rede-iot iot-middleware \
  java br.uefs.middleware.sensor.implementation.Main
```

---

## ⚙️ Especificação do Protocolo de Mensagem

A comunicação via Socket transmite objetos serializados em **JSON**. Todas as mensagens seguem um contrato base para garantir a consistência no roteamento feito pelo Broker.

**Exemplo de payload de publicação:**

```json
{
  "operation": "PUBLISH",
  "topic": "ambiente/temperatura",
  "payload": {
    "value": 26.5,
    "unit": "C"
  },
  "timestamp": "2026-04-05T16:00:00Z"
}
```

---

## 🛠 Tecnologias Aplicadas

| Tecnologia | Uso |
|------------|-----|
| **Java SE** | Desenvolvimento da lógica orientada a objetos |
| **Sockets TCP/IP** | Camada de transporte confiável para a infraestrutura de rede |
| **Multithreading** | Paralelismo no Broker para atendimento concorrente e não-bloqueante |
| **JSON** | Serialização e desserialização de pacotes de dados |
| **Docker** | Containerização para padronização do ambiente de execução |
