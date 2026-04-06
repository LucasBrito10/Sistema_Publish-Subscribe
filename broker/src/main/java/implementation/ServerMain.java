package implementation;

public class ServerMain {

    public static void main(String[] args) {
        int porta = 8080; // Você pode escolher a porta que preferir

        System.out.println("=== Iniciando o Broker (Servidor) ===");
        System.out.println("Configurando servidor na porta: " + porta);

        // Instancia o Broker na porta especificada
        Broker broker = new Broker(porta);

        System.out.println("[OK] Servidor rodando e aguardando conexões...");

        // Inicia o loop infinito que aceita clientes
        broker.runServer(true); 
    }
}