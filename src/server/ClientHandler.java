package server;

import resources.Protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Player player;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket, Player player) {
        this.socket = socket;
        this.player = player;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    // Espera hasta timeoutSeconds segundos a que el cliente envíe un mensaje de tiro.
    public String waitForShot(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000;
        try {
            while (System.currentTimeMillis() < endTime) {
                if (in.ready()) {
                    String line = in.readLine();
                    if (line != null && line.startsWith(Protocol.TIRO_PREFIX)) {
                        return line;
                    }
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void run() {
        /* En este ejemplo la mayoría de la lógica se gestiona en GameServer;
        aquí se podría incluir la lectura asíncrona de otros mensajes. */
    }
}
