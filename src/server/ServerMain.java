// src/server/ServerMain.java
package server;

import gui.GUIServer;
import javax.swing.SwingUtilities;

public class ServerMain {
    public static void main(String[] args) {
        int port = 12345; // Example port number
        GameServer server = new GameServer(port, null); // Pass null as the second parameter

        // Initialize the boards
        Board enemyBoard = new Board(10); // Example size
        Board ownBoard = new Board(10); // Example size

        // Start the GUIServer interface
        SwingUtilities.invokeLater(() -> {
            GUIServer gui = new GUIServer(server);
            server.setGuiServer(gui);
            gui.setVisible(true);

            // Update the GUI with the boards
            gui.updateBoard(enemyBoard, ownBoard, "player1");

            // Start the server logic after the GUI is visible
            new Thread(server::start).start();
        });
    }
}