// src/server/ServerMain.java
package server;

import gui.GUIServer;
import javax.swing.SwingUtilities;

public class ServerMain {
    public static void main(String[] args) {
        int port = 12345;
        GameServer server = new GameServer(port, null);

        Board enemyBoard = new Board(10);
        Board ownBoard = new Board(10);

        SwingUtilities.invokeLater(() -> {
            GUIServer gui = new GUIServer(server);
            server.setGuiServer(gui);
            gui.setVisible(true);

            gui.updateBoard(enemyBoard, ownBoard, "player1");

            new Thread(server::start).start();
        });
    }
}