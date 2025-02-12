package client;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Client {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String readMessage() {
        try {
            return in.readLine();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public BufferedReader getReader() {
        return in;
    }

    public PrintWriter getWriter() {
        return out;
    }

    public void close() {
        try {
            socket.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
