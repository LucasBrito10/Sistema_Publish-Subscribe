package implementation;

import enums.*;

import java.util.Scanner;

public abstract class Sensor extends Client {
    protected String sensorId;
    protected TopicType topic;
    protected int delay;
    protected volatile boolean isCritical = false;

    public void initializeSensor(String id, String host, int port, TopicType topic, int delay) {
        this.sensorId = id;
        this.topic = topic;
        this.delay = delay;
        this.setId(id);
        this.startPublisher(host, port, topic);
    }

    public void runSensor() {
        //THREAD REPONSÁVEL PELA ESCOLHA DO TIPO DE DADOS
        Thread inputThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("=== SENSOR " + this.sensorId + " ===");
                System.out.println("1 - Modo Normal");
                System.out.println("2 - Modo Critico (Anomalia)");
                System.out.println("3 - Encerrar Sensor");
                
                while (true) {
                    if (scanner.hasNextLine()) {
                        String choice = scanner.nextLine();
                        if (choice.equals("2")) {
                            this.isCritical = true;
                            System.out.println("[" + this.sensorId + "] Modo CRITICO.");
                        } else if (choice.equals("1")) {
                            this.isCritical = false;
                            System.out.println("[" + this.sensorId + "] Modo NORMAL.");
                        } else if (choice.equals("3")) {
                            System.exit(0);
                        }
                    }
                }
            }
        });
        inputThread.start();
        //THREAD PARA GERAR DADOS DE TELEMETRIA
        try {
            while (true) {
                this.sendMessage(generateData(this.isCritical), this.topic);
                Thread.sleep(this.delay);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected abstract String generateData(boolean isCritical);
}
