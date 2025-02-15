package gui;

import client.Client;
import client.ClientListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class GUIMain extends JFrame implements ClientListener {
    private Client client;
    private JTextArea textArea;
    private JPanel boardPanel;
    private int boardSize = 10; // Valor por defecto; se actualizará al recibir "#TAB,n#"
    private boolean myTurn = false;
    // Variables para recordar la última celda disparada
    private int lastShotRow = -1, lastShotCol = -1;

    public GUIMain(Client client) {
        this.client = client;
        // Intentamos usar el Look and Feel Nimbus para una apariencia más moderna.
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch(Exception e) {
            // Si falla, se mantiene el L&F por defecto.
        }
        setTitle("Hundir la Flota - Cliente");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        // Panel principal con espaciado
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Área de texto para mensajes
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(600, 150));
        mainPanel.add(scroll, BorderLayout.SOUTH);

        // Panel del tablero con un GridLayout y márgenes entre celdas
        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(boardSize, boardSize, 2, 2));
        initializeBoard();
        mainPanel.add(boardPanel, BorderLayout.CENTER);
    }

    // Crea la cuadrícula de botones del tablero.
    private void initializeBoard() {
        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(boardSize, boardSize, 2, 2));
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                JButton btn = new JButton();
                btn.setFont(new Font("SansSerif", Font.BOLD, 12));
                btn.setBackground(Color.LIGHT_GRAY);
                final int r = row;
                final int c = col;
                btn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (myTurn) {
                            lastShotRow = r;
                            lastShotCol = c;
                            // Se deshabilita el botón para evitar múltiples disparos en el mismo turno.
                            btn.setEnabled(false);
                            // Se envía el mensaje de tiro.
                            String msg = "#TIRO(" + convertRowToLetter(r) + "," + (c + 1) + ")#";
                            client.sendMessage(msg);
                            appendMessage("Enviando tiro: " + msg);
                            myTurn = false;
                        } else {
                            appendMessage("No es tu turno.");
                        }
                    }
                });
                boardPanel.add(btn);
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private char convertRowToLetter(int row) {
        return (char) ('A' + row);
    }

    // Actualiza el tamaño del tablero y lo reconstruye.
    private void updateBoardSize(int newSize) {
        boardSize = newSize;
        initializeBoard();
        appendMessage("Tamaño de tablero actualizado: " + newSize);
    }

    private void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(msg + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    // Actualiza el botón correspondiente a la última celda disparada según el resultado.
    private void updateButtonWithShotResult(int row, int col, String result) {
        SwingUtilities.invokeLater(() -> {
            int index = row * boardSize + col;
            Component comp = boardPanel.getComponent(index);
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (result.equals("#AGUA#")) {
                    btn.setBackground(Color.CYAN);
                    btn.setText("Agua");
                } else if (result.equals("#TOCADO#")) {
                    btn.setBackground(Color.ORANGE);
                    btn.setText("Tocado");
                } else if (result.equals("#HUNDIDO#")) {
                    btn.setBackground(Color.RED);
                    btn.setText("Hundido");
                }
                btn.setEnabled(false);
            }
        });
    }

    // Se invoca cada vez que se recibe un mensaje del servidor.
    @Override
    public void onMessageReceived(String message) {
        appendMessage("Servidor: " + message);
        if (message.startsWith("#TAB,")) {
            // Ejemplo: "#TAB,10#"
            try {
                int start = message.indexOf(",") + 1;
                int end = message.indexOf("#", start);
                int size = Integer.parseInt(message.substring(start, end));
                updateBoardSize(size);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (message.startsWith("#POS%")) {
            appendMessage("Posiciones de barcos recibidas.");
        } else if (message.equals("#INICIO#")) {
            appendMessage("El juego ha comenzado.");
        } else if (message.startsWith("#turno#")) {
            myTurn = true;
            appendMessage("¡Es tu turno! Selecciona una casilla para disparar.");
        } else if (message.equals("#AGUA#") || message.equals("#TOCADO#") || message.equals("#HUNDIDO#")) {
            // Se actualiza el botón que corresponde a la última celda seleccionada.
            updateButtonWithShotResult(lastShotRow, lastShotCol, message);
            appendMessage("Resultado del tiro: " + message.replace("#", ""));
        } else if (message.startsWith("#BARCO,")) {
            appendMessage("Barco hundido: " + message);
        } else if (message.startsWith("#FIN#")) {
            appendMessage("Jugador eliminado: " + message);
        } else if (message.startsWith("#GANADOR#")) {
            appendMessage("Ganador: " + message);
            JOptionPane.showMessageDialog(this, "¡Ha ganado el juego!");
        }
    }
}
