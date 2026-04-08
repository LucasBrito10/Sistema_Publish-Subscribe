package implementation;
import enums.*;

public class SensorTemperature extends Sensor {
    private int temp = 25;

    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("BROKER_HOST", "localhost");
        int port    = Integer.parseInt(System.getenv().getOrDefault("BROKER_PORT", "8080"));

        SensorTemperature sensor = new SensorTemperature();
        sensor.initializeSensor("SENSOR-TEMP", host, port, TopicType.TEMP, 2000);
        System.out.println("Sensor Temperature initialized and listening.");
        sensor.runSensor();
    }

    @Override
    protected String generateData(boolean isCritical) {
        if (isCritical) {
            temp = (temp < 40) ? 40 : temp + 5;
            if (temp > 60) temp = 40;
        } else {
            temp = (temp >= 35 || temp < 20) ? 20 : temp + 1;
        }
        return "Temperature: " + temp + "C";
    }
}
