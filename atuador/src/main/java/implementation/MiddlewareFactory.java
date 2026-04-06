package implementation;

import java.io.IOException;

public class MiddlewareFactory {

    public static MiddlewareClient createClient(String host, int port, String id) {
        try {
            return new MiddlewareClient(host, port, id);
        } catch (IOException e) {
            return null;
        }
    }
}