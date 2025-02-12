package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class BufferedReaderWrapper {
    private BufferedReader reader;

    public BufferedReaderWrapper(Socket socket) throws Exception {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String readLine() throws Exception {
        return reader.readLine();
    }
}
