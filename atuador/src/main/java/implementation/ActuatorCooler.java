package implementation;
import enums.TopicType;

public class ActuatorCooler extends Actuator {
    public static void main(String[] args) {
        ActuatorCooler cooler = new ActuatorCooler();
        cooler.initializeActuator("ACTUATOR-COOLER", "localhost", 8080, TopicType.TEMP);
        System.out.println("Actuator Cooler initialized and listening.");
    }
}