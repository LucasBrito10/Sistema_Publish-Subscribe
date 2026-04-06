package implementation;
import enums.TopicType;
import data_type.*;

public interface Middleware {
    public void sendMessage(Message message);
    public Message receiveMessage(String jsonString);
    public void publish(boolean restriction);
    public void subscribe(boolean restriction);
    public void registerCallback();
    public void setTopic(TopicType topic);
}