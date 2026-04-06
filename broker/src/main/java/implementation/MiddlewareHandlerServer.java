package implementation;

import data_type.Message;
import enums.CommandType;
import enums.TopicType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class MiddlewareHandlerServer implements Runnable {
    private Socket clientSocket;
    private String nodeId;
    private Broker broker;
    private String connectionType; 
    private TopicType topic;
    private BufferedReader in;
    private PrintWriter out;
    private ObjectMapper mapper;

    public MiddlewareHandlerServer(Socket socket, Broker broker) {
        try {
            this.clientSocket = socket;
            this.broker = broker;
            this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
            this.mapper = new ObjectMapper();
            this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) {
        try {
            String jsonPayload = this.mapper.writeValueAsString(message);
            this.out.println(jsonPayload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Message receiveMessage() {
        try {
            String jsonString = this.in.readLine();
            if (jsonString == null) return null; 
            return this.mapper.readValue(jsonString, Message.class);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void run() {
        try {
            Message initialMessage = receiveMessage();
            if (initialMessage == null) return;
            this.nodeId = initialMessage.getNodeId();
            this.topic = initialMessage.getTopic();
            this.connectionType = initialMessage.getConnectionType();
            this.broker.addConnection(this.topic, this.nodeId, this);

            Message incomingMessage;
            while ((incomingMessage = receiveMessage()) != null) {
                this.broker.processMessage(incomingMessage);
                checkCriticalLevels(incomingMessage);
            }
        } catch (Exception e) {
        } finally {
            this.broker.removeConnection(this.topic, this.nodeId, this);
        }
    }

    private void checkCriticalLevels(Message msg) {
        if (msg.getData() != null && msg.getTopic() != null) {
            String data = msg.getData();
            if (msg.getTopic() == TopicType.TEMP && data.startsWith("Temperature:")) {
                int val = Integer.parseInt(data.replaceAll("[^0-9]", ""));
                if (val >= 40) sendControlCommand("ACTUATOR-COOLER", CommandType.TURN_OFF, TopicType.TEMP);
            } else if (msg.getTopic() == TopicType.UMI && data.startsWith("Humidity:")) {
                int val = Integer.parseInt(data.replaceAll("[^0-9]", ""));
                if (val >= 80) sendControlCommand("ACTUATOR-EXHAUST", CommandType.TURN_OFF, TopicType.UMI);
            }
        }
    }

    private void sendControlCommand(String targetId, CommandType cmdType, TopicType targetTopic) {
        Message cmdMsg = new Message();
        cmdMsg.setNodeId("SERVER_HANDLER");
        cmdMsg.setConnectionType("PUBLISHER");
        cmdMsg.setTopic(targetTopic);
        cmdMsg.setCommandType(cmdType);
        cmdMsg.setTargetNodeId(targetId);
        cmdMsg.setData("CRITICAL_LIMIT_REACHED");
        this.broker.processMessage(cmdMsg);
    }

    public String getNodeId() { return nodeId; }
    public String getConnectionType() { return connectionType; }
    public TopicType getTopic() { return topic; }
}