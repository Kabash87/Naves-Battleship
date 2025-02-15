// src/gui/GUIServer.java
package gui;

import server.Board;
import server.GameServer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GUIServer extends JFrame {
    private GameServer server;
    private JTextArea textArea;
    private JPanel boardPanel;
    private JLabel boardTitleLabel;
    private int boardSize = 10; // Default value; will be updated upon receiving "#TAB,n#"

    public GUIServer(GameServer server) {
        this.server = server;
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch(Exception e) {
            // If Nimbus is not available, fall back to default
        }
        setTitle("Naves-Battleship - Server");
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(800, 150));
        mainPanel.add(scroll, BorderLayout.SOUTH);

        boardTitleLabel = new JLabel("Tablero de jugador1", SwingConstants.CENTER);
        boardTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        mainPanel.add(boardTitleLabel, BorderLayout.NORTH);

        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(boardSize, boardSize, 2, 2));
        initializeBoard();
        mainPanel.add(boardPanel, BorderLayout.CENTER);
    }

    private void initializeBoard() {
        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(boardSize, boardSize, 2, 2));
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                JButton btn = new JButton();
                btn.setFont(new Font("SansSerif", Font.BOLD, 12));
                btn.setBackground(Color.LIGHT_GRAY);
                btn.setEnabled(true); // Enable buttons on the server
                boardPanel.add(btn);
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    public void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    public void updateBoard(Board board, Board ownBoard, String player) {
        SwingUtilities.invokeLater(() -> {
            boardTitleLabel.setText("Tablero de " + player);
            boardPanel.removeAll();
            int size = board.getSize();
            boardPanel.setLayout(new GridLayout(size, size));
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    JButton cell = new JButton();
                    cell.setEnabled(true); // Enable buttons on the server
                    if (ownBoard.isShipAt(i, j)) {
                        if (ownBoard.isHitAt(i, j)) {
                            cell.setBackground(Color.MAGENTA); // Change to magenta for "tocado" on own ships
                            cell.setText("tocado");
                        } else {
                            cell.setBackground(Color.GRAY);
                            cell.setText("enemigo"); // enemigo
                        }
                    } else if (board.isShipAt(i, j)) {
                        cell.setBackground(Color.GREEN); // Aliado
                        cell.setText("aliado");
                    } else if (board.isHitAt(i, j)) {
                        cell.setBackground(Color.ORANGE); // Change to orange for "tocado"
                        cell.setText("tocado");
                    } else if (board.isMissAt(i, j)) {
                        cell.setBackground(Color.BLUE);
                        cell.setText("agua");
                    } else {
                        cell.setBackground(Color.WHITE);
                    }
                    boardPanel.add(cell);
                }
            }
            boardPanel.revalidate();
            boardPanel.repaint();
        });
    }
}