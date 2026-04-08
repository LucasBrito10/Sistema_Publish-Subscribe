package implementation;
import enums.TopicType;

public class ActuatorExhaust extends Actuator {
    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("BROKER_HOST", "localhost");
        int port    = Integer.parseInt(System.getenv().getOrDefault("BROKER_PORT", "8080"));

        ActuatorExhaust exhaust = new ActuatorExhaust();
        exhaust.initializeActuator("ACTUATOR-EXHAUST", host, port, TopicType.UMI);
        System.out.println("Actuator Exhaust initialized and listening.");
    }
}
