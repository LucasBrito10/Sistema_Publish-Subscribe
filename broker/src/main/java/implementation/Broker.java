package implementation;

import data_type.Message;
import enums.TopicType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class Broker {
    private ServerSocket serverSocket;
    private Map<TopicType, Set<MiddlewareHandlerServer>> clientsPerTopic;
    private Map<String, MiddlewareHandlerServer> clientsById;

    public Broker(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            this.clientsPerTopic = new HashMap<>(); 
            this.clientsById = new HashMap<>(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addConnection(TopicType topic, String nodeId, MiddlewareHandlerServer handler) {
        if (topic != null) {
            this.clientsPerTopic.putIfAbsent(topic, new HashSet<>());
            this.clientsPerTopic.get(topic).add(handler);
        }
        
        if (nodeId != null && !nodeId.isEmpty()) {
            this.clientsById.put(nodeId, handler);
        }
    }

    public synchronized void removeConnection(TopicType topic, String nodeId, MiddlewareHandlerServer handler) {
        if (topic != null) {
            Set<MiddlewareHandlerServer> connections = this.clientsPerTopic.get(topic);
            if (connections != null) {
                connections.remove(handler); 
                if (connections.isEmpty()) {
                    this.clientsPerTopic.remove(topic);
                }
            }
        }
        
        if (nodeId != null) {
            this.clientsById.remove(nodeId);
        }
    }

    public void processMessage(Message message) {
        if (message == null) return;

        if (message.getTargetNodeId() != null && !message.getTargetNodeId().isEmpty()) {
            boolean messageDelivered = false;
            TopicType targetTopic = message.getTopic();
            
            if (targetTopic != null && this.clientsPerTopic.containsKey(targetTopic)) {
                for (MiddlewareHandlerServer client : this.clientsPerTopic.get(targetTopic)) {
                    if (client.getNodeId().equals(message.getTargetNodeId()) && "SUBSCRIBER".equals(client.getConnectionType())) {
                        client.sendMessage(message);
                        messageDelivered = true;
                        break;
                    }
                }
            }
            
            if (!messageDelivered) {
                MiddlewareHandlerServer targetClient = this.clientsById.get(message.getTargetNodeId());
                if (targetClient != null) {
                    targetClient.sendMessage(message);
                }
            }
            return; 
        }

        TopicType targetTopic = message.getTopic();
        if (targetTopic == null) return;
        
        Set<MiddlewareHandlerServer> connections = this.clientsPerTopic.get(targetTopic);
        if (connections == null) return;

        for (MiddlewareHandlerServer client : connections) {
            if ("SUBSCRIBER".equals(client.getConnectionType())) {
                client.sendMessage(message);
            }
        }
    }

    public void runServer(boolean isRunning) {
        if (!isRunning) return;
        try {
            while (isRunning) {
                Socket clientSocket = this.serverSocket.accept();
                System.out.println("[OK] conexão recebida...");
                MiddlewareHandlerServer handler = new MiddlewareHandlerServer(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}