package server;

import java.io.PrintWriter;
import java.net.Socket;

public class Player {
    private String username;
    private Socket socket;
    private PrintWriter out;
    private Board board;
    private boolean active;

    public Player(String username, Socket socket, PrintWriter out, Board board) {
        this.username = username;
        this.socket = socket;
        this.out = out;
        this.board = board;
        this.active = true;
    }

    public String getUsername() {
        return username;
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
