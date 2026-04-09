package implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import enums.TopicType;
import data_type.Message;

public class MiddlewareClient implements Middleware, Runnable {
    private Socket socket;
    private TopicType topic;
    private String idClient;
    private String connectionType;
    private BufferedReader in;
    private PrintWriter out;
    private ObjectMapper mapper;
    private BlockingQueue<Message> sendQueue;
    private BlockingQueue<Message> receiveQueue;

    public MiddlewareClient(String ipBroker, int port, String idClient) throws IOException {
        this.idClient = idClient;
        this.socket = new Socket(ipBroker, port);
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.out = new PrintWriter(this.socket.getOutputStream(), true);
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.sendQueue = new LinkedBlockingQueue<>();
        this.receiveQueue = new LinkedBlockingQueue<>();
    }

    //MÉTODO RUN PARA ESTABELECER O QUE A THREAD VAI FAZER, NO CASO DESTE PROJETO, SOMENTE A THREAD DE RECEBIMENTO DE MENSAGEM UTILIZA ESTE MÉTODO RUN
    @Override
    public void run() {
        try {
            String json;
            while ((json = this.in.readLine()) != null) {
                Message msg = this.receiveMessage(json);
                if (msg != null) this.receiveQueue.put(msg);
            }
        } catch (Exception e) {}
    }

    //INICIAR CONEXÃO PUB
    @Override
    public void publish(boolean restriction) {
        if (restriction) return;
        this.connectionType = "PUBLISHER";
        try {
            //HANDSHAKE -> QUANDO A CONEXÃO É ABERTA, MANDA MENSAGEM PARA O SERVIDOR COM OS DADOS DO   
            Message reg = new Message();
            reg.setNodeId(this.idClient);
            reg.setConnectionType(this.connectionType);
            reg.setTopic(this.topic);
            this.out.println(this.mapper.writeValueAsString(reg));

            //THREAD PARA ENVIO DE MENSAGENS
            new Thread(() -> {
                try {
                    while (true) {
                        this.out.println(this.mapper.writeValueAsString(this.sendQueue.take()));
                    }
                } catch (Exception e) {}
            }).start();
        } catch (Exception e) {}
    }

    //INICIA CONEXÃO DO TIPO SUB

    @Override
    public void subscribe(boolean restriction) {
        if (restriction) return;
        this.connectionType = "SUBSCRIBER";
        try {
            Message reg = new Message();
            reg.setNodeId(this.idClient);
            reg.setConnectionType(this.connectionType);
            reg.setTopic(this.topic);
            this.out.println(this.mapper.writeValueAsString(reg));
            
            new Thread(this).start();
        } catch (Exception e) {}
    }

    @Override
    public Message receiveMessage(String json) {
        try { return this.mapper.readValue(json, Message.class); } catch (Exception e) { return null; }
    }

    @Override
    public void sendMessage(Message message) {
        try { this.sendQueue.put(message); } catch (Exception e) {}
    }

    public Message receiveMessageFromQueue() throws InterruptedException {
        return this.receiveQueue.take();
    }

    public void setTopic(TopicType topic) { this.topic = topic; }
    @Override public void registerCallback() {}
}
