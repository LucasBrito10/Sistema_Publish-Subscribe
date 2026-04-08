package implementation;
import enums.*;
import data_type.*;

import java.util.Scanner;

// Interface de terminal para o cliente IoT
// Fornece menu interativo para o usuário
public class ClientTerminalUI {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // Cria novo cliente
            Client client = new Client();
            System.out.println("=== Gateway IoT ===");
            
            // Solicita dados de conexão ao servidor
            System.out.print("Host: ");
            String host = scanner.nextLine();
            System.out.print("Port: ");
            int port = Integer.parseInt(scanner.nextLine());
            System.out.print("Device ID: ");
            String id = scanner.nextLine();
            // Define o ID do cliente
            client.setId(id);

            // Loop principal do menu
            while (true) {
                System.out.println("\n--- MENU ---");
                System.out.println("1. Subscribe to Topic");
                System.out.println("2. Create Publisher");
                System.out.println("3. Send Telemetry");
                System.out.println("4. Send Command");
                System.out.println("5. Show History");
                System.out.println("6. Exit");
                System.out.print("Option: ");
                String option = scanner.nextLine();

                try {
                    switch (option) {
                        // Opção 1: Subscriber para um tópico
                        case "1":
                            System.out.print("Topic (TEMP/UMI): ");
                            TopicType subTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            // Tenta iniciar subscriber
                            if (client.startSubscribe(host, port, subTopic)) {
                                System.out.println("Listening [" + subTopic +"]...");
                            }
                            break;
                        
                        // Opção 2: Criar publisher para um tópico
                        case "2":
                            System.out.print("Topic (TEMP/UMI): ");
                            TopicType pubTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            // Tenta iniciar publicador
                            if (client.startPublisher(host, port, pubTopic)) {
                                System.out.println("Publisher created for [" + pubTopic + "].");
                            }
                            break;
                        
                        // Opção 3: Enviar mensagem de telemetria
                        case "3":
                            System.out.print("Target Topic: ");
                            TopicType targetTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            System.out.print("Message: ");
                            String textData = scanner.nextLine();
                            // Envia a mensagem
                            client.sendMessage(textData, targetTopic);
                            break;
                        
                        // Opção 4: Enviar comando e aguardar resposta
                        case "4":
                            System.out.print("Network Topic (TEMP/UMI): ");
                            TopicType cmdTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            
                            // Garante que tem publicador e subscriber para receber respostas
                            client.startPublisher(host, port, cmdTopic);
                            client.startSubscribe(host, port, cmdTopic);

                            // Solicita informações do comando
                            System.out.print("Target Actuator ID: ");
                            String targetId = scanner.nextLine();
                            System.out.println("Commands: 1-TURN_ON | 2-TURN_OFF | 3-STATUS_REQUEST");
                            System.out.print("Choice: ");
                            String cmdChoice = scanner.nextLine();
                            
                            // Define qual comando enviar
                            CommandType selectedCmd = CommandType.STATUS_REQUEST;
                            if (cmdChoice.equals("1")) selectedCmd = CommandType.TURN_ON;
                            if (cmdChoice.equals("2")) selectedCmd = CommandType.TURN_OFF;
                            
                            // Envia o comando
                            client.sendCommand("", cmdTopic, targetId, selectedCmd);
                            System.out.println("Command sent. Waiting for [" + targetId + "] to reply...");
                            
                            // Aguarda resposta por até 5 segundos
                            Message response = client.waitForResponse(targetId, 5000);
                            
                            // Exibe o resultado
                            if (response != null) {
                                System.out.println("=== CONFIRMATION RECEIVED ===");
                                System.out.println("Response from " + targetId + ": " + response.getData());
                            } else {
                                System.out.println("=== TIMEOUT ===");
                                System.out.println("No reply received from " + targetId + " in 5 seconds.");
                            }
                            break;
                        
                        // Opção 5: Exibir histórico de um tópico
                        case "5":
                            System.out.print("Topic: ");
                            TopicType historyTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            // Mostra o histórico de mensagens
                            client.displayTopicData(historyTopic);
                            break;
                        
                        // Opção 6: Sair do programa
                        case "6":
                            System.exit(0);
                    }
                } catch (IllegalArgumentException e) {
                    // Se o usuário inserir um tópico inválido
                    System.out.println("Invalid Topic.");
                }
            }
        }
    }
}