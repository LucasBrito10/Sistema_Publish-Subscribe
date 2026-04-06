package implementation;
import enums.*;


public class SensorHumidity extends Sensor {
    private int humidity = 40;

    public static void main(String[] args) {
        SensorHumidity sensor = new SensorHumidity();
        sensor.initializeSensor("SENSOR-HUMIDITY", "localhost", 8080, TopicType.UMI, 3000);
        System.out.println("Sensor Humidity initialized and listening.");
        sensor.runSensor();
    }

    @Override
    protected String generateData(boolean isCritical) {
        if (isCritical) {
            humidity = (humidity < 80) ? 80 : humidity + 2;
            if (humidity > 95) humidity = 80;
        } else {
            humidity = (humidity >= 60 || humidity < 40) ? 40 : humidity + 2;
        }
        return "Humidity: " + humidity + "%";
    }
}