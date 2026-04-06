package implementation;
import enums.*;
import data_type.*;

import java.util.Scanner;

public class ClientTerminalUI {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Client client = new Client();
            System.out.println("=== Gateway IoT ===");
            System.out.print("Host: ");
            String host = scanner.nextLine();
            System.out.print("Port: ");
            int port = Integer.parseInt(scanner.nextLine());
            System.out.print("Device ID: ");
            String id = scanner.nextLine();
            client.setId(id);

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
                        case "1":
                            System.out.print("Topic (TEMP/UMI): ");
                            TopicType subTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            if (client.startSubscribe(host, port, subTopic)) {
                                System.out.println("Listening [" + subTopic + "]...");
                            }
                            break;
                        case "2":
                            System.out.print("Topic (TEMP/UMI): ");
                            TopicType pubTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            if (client.startPublisher(host, port, pubTopic)) {
                                System.out.println("Publisher created for [" + pubTopic + "].");
                            }
                            break;
                        case "3":
                            System.out.print("Target Topic: ");
                            TopicType targetTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            System.out.print("Message: ");
                            String textData = scanner.nextLine();
                            client.sendMessage(textData, targetTopic);
                            break;
                        case "4":
                            System.out.print("Network Topic (TEMP/UMI): ");
                            TopicType cmdTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            
                            client.startPublisher(host, port, cmdTopic);
                            client.startSubscribe(host, port, cmdTopic);

                            System.out.print("Target Actuator ID: ");
                            String targetId = scanner.nextLine();
                            System.out.println("Commands: 1-TURN_ON | 2-TURN_OFF | 3-STATUS_REQUEST");
                            System.out.print("Choice: ");
                            String cmdChoice = scanner.nextLine();
                            
                            CommandType selectedCmd = CommandType.STATUS_REQUEST;
                            if (cmdChoice.equals("1")) selectedCmd = CommandType.TURN_ON;
                            if (cmdChoice.equals("2")) selectedCmd = CommandType.TURN_OFF;
                            
                            client.sendCommand("", cmdTopic, targetId, selectedCmd);
                            System.out.println("Command sent. Waiting for [" + targetId + "] to reply...");
                            
                            Message response = client.waitForResponse(targetId, 5000);
                            
                            if (response != null) {
                                System.out.println("=== CONFIRMATION RECEIVED ===");
                                System.out.println("Response from " + targetId + ": " + response.getData());
                            } else {
                                System.out.println("=== TIMEOUT ===");
                                System.out.println("No reply received from " + targetId + " in 5 seconds.");
                            }
                            break;
                        case "5":
                            System.out.print("Topic: ");
                            TopicType historyTopic = TopicType.valueOf(scanner.nextLine().toUpperCase());
                            client.displayTopicData(historyTopic);
                            break;
                        case "6":
                            System.exit(0);
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid Topic.");
                }
            }
        }
    }
}