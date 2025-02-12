package client;

public class ClientReaderThread extends Thread {
    private Client client;
    private ClientListener listener;

    public ClientReaderThread(Client client, ClientListener listener) {
        this.client = client;
        this.listener = listener;
    }

    public void run() {
        try {
            String line;
            while ((line = client.readMessage()) != null) {
                listener.onMessageReceived(line);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
