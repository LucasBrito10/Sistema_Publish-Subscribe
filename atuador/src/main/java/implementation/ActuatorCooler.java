package implementation;
import enums.TopicType;

public class ActuatorCooler extends Actuator {
    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("BROKER_HOST", "localhost");
        int port    = Integer.parseInt(System.getenv().getOrDefault("BROKER_PORT", "8080"));

        ActuatorCooler cooler = new ActuatorCooler();
        cooler.initializeActuator("ACTUATOR-COOLER", host, port, TopicType.TEMP);
        System.out.println("Actuator Cooler initialized and listening.");
    }
}
