package implementation;

import java.util.HashMap;
import java.util.Map;
import data_type.Message;
import enums.CommandType;
import enums.TopicType;
import java.util.LinkedList;
import java.util.List;

public class Client {
    private Map<TopicType, Middleware> publishers = new HashMap<>();
    private Map<TopicType, Middleware> subscribers = new HashMap<>();
    private Map<TopicType, LinkedList<String>> topicHistory = new HashMap<>();
    private String id;
    private volatile Message responseMessage = null;
    private final Object syncLock = new Object();

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public boolean startPublisher(String host, int port, TopicType topic) {
        if (this.publishers.containsKey(topic)) return true;
        try {
            Middleware pub = MiddlewareFactory.createClient(host, port, this.id);
            if (pub == null) return false;
            pub.setTopic(topic);
            pub.publish(false); 
            this.publishers.put(topic, pub);
            this.startListening(pub);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean startSubscribe(String host, int port, TopicType topic) {
        if (this.subscribers.containsKey(topic)) return true;
        try {
            Middleware sub = MiddlewareFactory.createClient(host, port, this.id);
            if (sub == null) return false;
            sub.setTopic(topic);
            sub.subscribe(false); 
            this.subscribers.put(topic, sub);
            this.topicHistory.putIfAbsent(topic, new LinkedList<>());
            this.startListening(sub);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void startListening(Middleware middleware) {
        new Thread(() -> {
            try {
                while (true) {
                    Message msg = ((MiddlewareClient) middleware).receiveMessageFromQueue();
                    if (msg != null) {
                        if (msg.getCommandType() == CommandType.RESPONSE) {
                            synchronized (syncLock) {
                                responseMessage = msg;
                                syncLock.notifyAll();
                            }
                        } else if (msg.getData() != null && msg.getCommandType() == CommandType.TELEMETRY) {
                            storeMessageData(msg.getTopic(), msg.getData());
                        }
                        handleIncomingMessage(msg);
                    }
                }
            } catch (Exception e) {
            }
        }).start();
    }

    protected void handleIncomingMessage(Message msg) {}

    private synchronized void storeMessageData(TopicType topic, String data) {
        LinkedList<String> history = this.topicHistory.get(topic);
        if (history != null) {
            if (history.size() >= 10) {
                history.removeFirst();
            }
            history.addLast(data);
        }
    }

    public void displayTopicData(TopicType topic) {
        List<String> history = this.topicHistory.get(topic);
        System.out.println("--------------------------------------------------");
        System.out.printf("| %-46s |\n", "TOPIC: " + topic);
        System.out.println("--------------------------------------------------");
        if (history == null || history.isEmpty()) {
            System.out.printf("| %-46s |\n", "NO DATA");
        } else {
            for (int i = 0; i < history.size(); i++) {
                System.out.printf("| %-5d | %-38s |\n", (i + 1), history.get(i));
            }
        }
        System.out.println("--------------------------------------------------");
    }

    public void sendMessage(String data, TopicType topic) {
        Middleware pub = this.publishers.get(topic);
        if (pub == null) return;
        Message msg = new Message();
        msg.setData(data);
        msg.setNodeId(this.id);
        msg.setConnectionType("PUBLISHER");
        msg.setTopic(topic);
        msg.setCommandType(CommandType.TELEMETRY);
        pub.sendMessage(msg);
    }

    public void sendCommand(String data, TopicType topic, String targetNodeId, CommandType type) {
        Middleware pub = this.publishers.get(topic);
        if (pub == null) return;
        Message msg = new Message();
        msg.setData(data);
        msg.setNodeId(this.id);
        msg.setConnectionType("PUBLISHER");
        msg.setTopic(topic);
        msg.setCommandType(type);
        msg.setTargetNodeId(targetNodeId); 
        pub.sendMessage(msg);
    }

    public void sendResponse(String data, TopicType topic, String targetNodeId) {
        Middleware mid = this.publishers.get(topic);
        if (mid == null) mid = this.subscribers.get(topic);
        if (mid == null) return;
        Message msg = new Message();
        msg.setData(data);
        msg.setNodeId(this.id);
        msg.setConnectionType("PUBLISHER");
        msg.setTopic(topic);
        msg.setCommandType(CommandType.RESPONSE);
        msg.setTargetNodeId(targetNodeId);
        mid.sendMessage(msg);
    }

    public Message waitForResponse(String expectedNodeId, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        synchronized (syncLock) {
            responseMessage = null;
            while (responseMessage == null || !responseMessage.getNodeId().equals(expectedNodeId)) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= timeoutMillis) return null;
                try {
                    syncLock.wait(timeoutMillis - elapsed);
                } catch (InterruptedException e) {}
            }
            return responseMessage;
        }
    }
}