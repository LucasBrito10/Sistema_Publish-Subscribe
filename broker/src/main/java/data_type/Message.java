package data_type;

import enums.CommandType;
import enums.TopicType;

public class Message {
    private String action;
    private String connectionType; 
    private TopicType topic;
    private String nodeId;
    private String data;
    private CommandType commandType;
    private String targetNodeId;

    public Message() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

    public TopicType getTopic() { return topic; }
    public void setTopic(TopicType topic) { this.topic = topic; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public CommandType getCommandType() { return commandType; }
    public void setCommandType(CommandType commandType) { this.commandType = commandType; }

    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
}