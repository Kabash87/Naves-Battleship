package client;

import gui.GUIMain;

public class ClientMain {
    public static void main(String[] args) {
        // Se conecta al servidor (por ejemplo, localhost en puerto 12345)
        Client client = new Client("localhost", 12345);
        if (client.connect()) {
            // Envía mensaje de registro: "#REG#"
            client.sendMessage("#REG#");
            // Crea y muestra la GUI
            GUIMain gui = new GUIMain(client);
            gui.setVisible(true);

            // Inicia el thread que escucha los mensajes del servidor
            ClientReaderThread readerThread = new ClientReaderThread(client, gui);
            readerThread.start();
        } else {
            System.out.println("No se pudo conectar al servidor.");
        }
    }
}
