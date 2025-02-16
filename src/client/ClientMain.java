package client;

import gui.GUIMain;
import javax.swing.*;
import java.awt.*;

public class ClientMain {
    public static void main(String[] args) {
        String[] connectionData = ConnectionForm.showConnectionForm();
        if (connectionData == null) {
            System.out.println("Conexión cancelada.");
            return;
        }
        
        String ip = connectionData[0];
        int port = Integer.parseInt(connectionData[1]);
        String username = connectionData[2];

        Client client = new Client(ip, port);
        if (client.connect()) {
            client.sendMessage("#REG#" + username);

            GUIMain gui = new GUIMain(client);
            gui.setVisible(true);

            ClientReaderThread readerThread = new ClientReaderThread(client, gui);
            readerThread.start();
        } else {
            System.out.println("No se pudo conectar al servidor.");
        }
    }
}

class ConnectionForm {
    public static String[] showConnectionForm() {
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("12345");
        JTextField usernameField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("IP:"));
        panel.add(ipField);
        panel.add(new JLabel("Puerto:"));
        panel.add(portField);
        panel.add(new JLabel("Nombre de Usuario:"));
        panel.add(usernameField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Conectar al Servidor",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            return new String[]{ipField.getText(), portField.getText(), usernameField.getText()};
        } else {
            return null;
        }
    }
}


