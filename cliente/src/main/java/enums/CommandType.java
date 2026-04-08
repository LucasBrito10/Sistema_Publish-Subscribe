package enums;

// Enumeração que define os tipos de comandos que podem ser enviados
// Cada tipo representa uma ação ou tipo de mensagem no sistema IoT
public enum CommandType {
    TURN_ON,      // Comando para ligar um dispositivo
    TURN_OFF,     // Comando para desligar um dispositivo
    STATUS_REQUEST,  // Solicitação de status de um dispositivo
    TELEMETRY,    // Dados de telemetria (sensores)
    RESPONSE      // Resposta a um comando anterior
}