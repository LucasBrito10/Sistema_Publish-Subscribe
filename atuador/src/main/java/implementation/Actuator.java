package implementation;
import enums.CommandType;
import enums.TopicType;
import data_type.Message;

public abstract class Actuator extends Client {
    protected boolean isOn = true;

    public void initializeActuator(String id, String host, int port, TopicType topic) {
        this.setId(id);
        this.startSubscribe(host, port, topic);
        this.startPublisher(host, port, topic); 
    }

    // MÉTODO QUE SOBREESCREVE O MÉTODO EXIGIDO PELA CLASSE CLIENTE PARA TRATAR CASOS ESPECÍFICOS DE MENSAGENS E COMO REAGIR 
    @Override
    protected void handleIncomingMessage(Message msg) {
        if (msg.getTargetNodeId() != null && msg.getTargetNodeId().equals(this.getId())) {
            //CASO DE COMANDO DE DESLIGAMENTO
            if (msg.getCommandType() == CommandType.TURN_OFF) {
                this.isOn = false;
                System.out.println("[" + this.getId() + "] Command TURN_OFF received. Status: OFF");
                sendResponse("ACK_TURN_OFF", msg.getTopic(), msg.getNodeId());
                //CASO DE COMANDO DE LIGAR
            } else if (msg.getCommandType() == CommandType.TURN_ON) {
                this.isOn = true;
                System.out.println("[" + this.getId() + "] Command TURN_ON received. Status: ON");
                sendResponse("ACK_TURN_ON", msg.getTopic(), msg.getNodeId());
                //CASO DE COMANDO DE REQUISIÇÃO DE STATUS
            } else if (msg.getCommandType() == CommandType.STATUS_REQUEST) {
                System.out.println("[" + this.getId() + "] Command STATUS_REQUEST received.");
                String statusData = this.isOn ? "STATUS: ON" : "STATUS: OFF";
                sendResponse(statusData, msg.getTopic(), msg.getNodeId());
            }
        }
    }
}
