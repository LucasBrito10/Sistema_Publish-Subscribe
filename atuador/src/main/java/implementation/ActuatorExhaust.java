package implementation;
import enums.TopicType;

public class ActuatorExhaust extends Actuator {
    public static void main(String[] args) {
        ActuatorExhaust exhaust = new ActuatorExhaust();
        exhaust.initializeActuator("ACTUATOR-EXHAUST", "localhost", 8080, TopicType.UMI);
        System.out.println("Actuator Exhaust initialized and listening.");
    }
}