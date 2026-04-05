# Distributed_Sistem 

Implementação de um Middleware baseado no modelo de comunicação Publish/Subscribe (Pub/Sub). O projeto foi desenvolvido em Java, utilizando Sockets TCP para garantir a entrega confiável de mensagens e JSON para a serialização dos dados. A arquitetura permite que múltiplos sensores (Publicadores) enviem dados para tópicos específicos, enquanto atuadores (Assinantes) recebem essas informações de forma assíncrona através de um Broker central, o que permite substituir a comunicação ponto-a-ponto, anteriormente utilizada, por uma comunicação cntralizadora e mais robusta que diminui o acoplamento entre os nós do sistema.

## 🏗 Arquitetura e Estrutura do Projeto

O sistema é descentralizado nas pontas, mas coordenado por um elemento centralizador de mensagens. O código-fonte está dividido em quatro pacotes principais para garantir a separação de responsabilidades:

* **`broker`**: Módulo central que recebe publicações, gerencia os tópicos ativos e encaminha as mensagens para os assinantes correspondentes.
* **`sensor`**: Módulo publicador (Publisher) que coleta dados ambientais ou de simulação e os envia ao Broker.
* **`atuador`**: Módulo assinante (Subscriber) que se inscreve em tópicos de interesse e reage às mensagens recebidas.
* **`cliente`**: Módulo base que encapsula a lógica de conexão TCP e abstrações comuns utilizadas por sensores e atuadores.

### Estrutura Interna dos Pacotes

Para manter a organização do domínio, cada um dos pacotes acima subdivide-se em:

* **`implementation/`**: Classes com a lógica de execução, instâncias de `ServerSocket`/`Socket` e controle de Threads.
* **`data_type/`**: Estruturas de dados, mapeamento de payloads e a classe modelo `Message`.
* **`enums/`**: Tipos enumerados de configuração, como tipos de operações de rede e categorias de sensores.

---

## 🚀 Guia de Execução

O sistema pode ser executado diretamente pelo terminal ou gerenciado via Maven/Docker. A ordem de inicialização é estrita: o Broker deve estar online antes de qualquer outro cliente tentar conexão.

### Pré-requisitos
* **Java JDK 17** (ou superior).
* **Maven** (para compilação e gestão de dependências).

### Compilação
Na raiz do projeto, execute:
```bash
mvn clean install
