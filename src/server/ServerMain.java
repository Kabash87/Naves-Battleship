package server;

public class ServerMain {
    public static void main(String[] args) {
        int port = 12345; // Puerto fijo para el programa
        GameServer server = new GameServer(port);
        server.start();
    }
}