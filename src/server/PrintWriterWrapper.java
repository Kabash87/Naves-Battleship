package server;

import java.io.PrintWriter;
import java.net.Socket;

public class PrintWriterWrapper {
    private PrintWriter writer;

    public PrintWriterWrapper(Socket socket) throws Exception {
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public void println(String msg) {
        writer.println(msg);
    }

    public PrintWriter getPrintWriter() {
        return writer;
    }
}
