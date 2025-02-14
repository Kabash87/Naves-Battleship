package gui;

import client.Client;
import client.ClientListener;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.awt.Point;

public class GUIMain extends JFrame implements ClientListener {
    private Client client;
    private JTextArea textArea;
    private JPanel boardPanel;
    private int boardSize = 10;
    private boolean myTurn = false;
    private int lastShotRow = -1, lastShotCol = -1;
    private SoundManager soundManager;
    private JButton muteButton;
    private JSlider volumeSlider;
    private Map<String, List<Point>> barcos = new HashMap<>();
    private Set<Point> posicionesTocadas = new HashSet<>();
    private static final String AGUA_ICON_PATH = "/resources/agua.png";
    private static final String TOCADO_ICON_PATH = "/resources/tocado.png";
    private static final String HUNDIDO_ICON_PATH = "/resources/hundido.png";

    VictoryAnimation victoryAnimation = new VictoryAnimation();

    // Atributo para controlar el modo oscuro/claro
    private boolean darkMode = false;

    public GUIMain(Client client) {
        this.client = client;
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) { }
        setTitle("Hundir la Flota - Cliente");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        soundManager = new SoundManager("/resources/music.wav");
        soundManager.play();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        getLayeredPane().add(victoryAnimation, JLayeredPane.POPUP_LAYER);
        victoryAnimation.setSize(200, 100);
        victoryAnimation.setVisible(false);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(600, 150));
        mainPanel.add(scroll, BorderLayout.SOUTH);

        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(boardSize, boardSize, 2, 2));
        initializeBoard();
        mainPanel.add(boardPanel, BorderLayout.CENTER);

        // 🔻 Panel superior para controles (música y tema) 🔻
        JPanel topPanel = new JPanel(new FlowLayout());

        // Botón para controlar el sonido
        muteButton = new JButton("🔊");
        muteButton.addActionListener(e -> {
            soundManager.toggleMute();
            muteButton.setText(soundManager.isMuted() ? "🔇" : "🔊");
        });
        topPanel.add(muteButton);

        // Control deslizante para el volumen
        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.addChangeListener(e -> {
            float volume = volumeSlider.getValue() / 100f;
            soundManager.setVolume(volume);
        });
        topPanel.add(new JLabel("Volumen:"));
        topPanel.add(volumeSlider);

        // Botón para alternar entre modo oscuro y claro
        JButton toggleThemeButton = new JButton("Modo Oscuro");
        toggleThemeButton.addActionListener(e -> {
            toggleTheme();
            // Actualizamos el texto del botón según el modo actual
            toggleThemeButton.setText(darkMode ? "Modo Claro" : "Modo Oscuro");
        });
        topPanel.add(toggleThemeButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);
    }

    // Método para alternar el tema de la interfaz
    private void toggleTheme() {
        darkMode = !darkMode;
        Color bg = darkMode ? Color.DARK_GRAY : Color.WHITE;
        Color fg = darkMode ? Color.WHITE : Color.BLACK;

        getContentPane().setBackground(bg);
        textArea.setBackground(bg);
        textArea.setForeground(fg);
        boardPanel.setBackground(bg);

        for (Component comp : boardPanel.getComponents()) {
            if (!(comp instanceof JButton)) { // Evitar modificar los botones
                comp.setBackground(bg);
                comp.setForeground(fg);
            }
        }
    }


    private void initializeBoard() {
        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(boardSize, boardSize, 2, 2));
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                JButton btn = new JButton();
                btn.setFont(new Font("SansSerif", Font.BOLD, 12));
                btn.setBackground(Color.LIGHT_GRAY); // Color fijo
                btn.setOpaque(true);
                btn.setBorderPainted(false);

                final int r = row;
                final int c = col;
                btn.addActionListener(e -> {
                    if (myTurn) {
                        lastShotRow = r;
                        lastShotCol = c;
                        btn.setEnabled(false);
                        String msg = "#TIRO(" + convertRowToLetter(r) + "," + (c + 1) + ")#";
                        client.sendMessage(msg);
                        appendMessage("Enviando tiro: " + msg);
                        myTurn = false;
                    } else {
                        appendMessage("No es tu turno.");
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

    private void updateButtonWithShotResult(int row, int col, String result) {
        SwingUtilities.invokeLater(() -> {
            int index = row * boardSize + col;
            Component comp = boardPanel.getComponent(index);

            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                int btnSize = Math.min(btn.getWidth(), btn.getHeight());
                ImageIcon icon = null;

                if (result.equals("#AGUA#")) {
                    icon = loadAndScaleIcon(AGUA_ICON_PATH, btnSize, btnSize);
                } else if (result.equals("#TOCADO#") || result.equals("#HUNDIDO#")) {
                    posicionesTocadas.add(new Point(row, col));
                    System.out.println(row + " " + col);
                    verificarSiBarcoHundido();

                    if (result.equals("#TOCADO#")) {
                        icon = loadAndScaleIcon(TOCADO_ICON_PATH, btnSize, btnSize);
                    } else {
                        icon = loadAndScaleIcon(HUNDIDO_ICON_PATH, btnSize, btnSize);
                    }
                }

                if (icon != null) {
                    btn.setIcon(icon);
                    btn.setDisabledIcon(icon);
                }
                btn.setEnabled(false);
            }
        });
    }

    private void cambiarIconoCasilla(int fila, int columna, String rutaIcono) {
        SwingUtilities.invokeLater(() -> {
            int index = fila * boardSize + columna;
            if (index < boardPanel.getComponentCount()) {
                Component comp = boardPanel.getComponent(index);
                if (comp instanceof JButton) {
                    JButton btn = (JButton) comp;
                    int btnSize = Math.min(btn.getWidth(), btn.getHeight());
                    ImageIcon icon = loadAndScaleIcon(rutaIcono, btnSize, btnSize);
                    btn.setIcon(icon);
                    btn.setDisabledIcon(icon);
                    btn.setEnabled(false);
                }
            }
        });
    }

    private ImageIcon loadAndScaleIcon(String path, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(path));
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("No se pudo cargar la imagen: " + path);
            return null;
        }
    }

    private void actualizarCasillaComoHundida(int row, int col) {
        SwingUtilities.invokeLater(() -> {
            int index = row * boardSize + col;
            Component comp = boardPanel.getComponent(index);

            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                int btnSize = Math.min(btn.getWidth(), btn.getHeight());
                btn.setIcon(loadAndScaleIcon(HUNDIDO_ICON_PATH, btnSize, btnSize));
                btn.setEnabled(false);
            }
        });
    }

    private void procesarPosicionesBarcos(String mensaje) {
        try {
            if (!mensaje.startsWith("RIVAL#POS%")) {
                appendMessage("⚠️ Mensaje con formato incorrecto: " + mensaje);
                return;
            }

            mensaje = mensaje.replace("RIVAL#POS%", "").replace("#", "");
            Pattern shipPattern = Pattern.compile("((?:\\(\\w,\\d+\\))+)");
            Matcher shipMatcher = shipPattern.matcher(mensaje);

            while (shipMatcher.find()) {
                String grupoBarco = shipMatcher.group(1);
                List<Point> coordenadas = new ArrayList<>();

                Matcher coordMatcher = Pattern.compile("\\((\\w),(\\d+)\\)").matcher(grupoBarco);
                while (coordMatcher.find()) {
                    char filaChar = coordMatcher.group(1).charAt(0);
                    int fila = filaChar - 'A';
                    int columna = Integer.parseInt(coordMatcher.group(2)) - 1;
                    coordenadas.add(new Point(fila, columna));
                }

                if (!coordenadas.isEmpty()) {
                    String nombreBarco = "Barco" + (barcos.size() + 1);
                    barcos.put(nombreBarco, coordenadas);
                    appendMessage("✅ Barco registrado: " + nombreBarco + " -> " + coordenadas);
                }
            }

        } catch (Exception e) {
            appendMessage("❌ Error procesando posiciones de barcos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarTableroConBarcos() {
        for (List<Point> posiciones : barcos.values()) {
            for (Point p : posiciones) {
                int index = p.x * boardSize + p.y;
                Component comp = boardPanel.getComponent(index);

                if (comp instanceof JButton) {
                    JButton btn = (JButton) comp;
                    int btnSize = Math.min(btn.getWidth(), btn.getHeight());
                    btn.setIcon(loadAndScaleIcon(HUNDIDO_ICON_PATH, btnSize, btnSize));
                    btn.setEnabled(false);
                }
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private void animateHit(JButton btn) {
        Timer timer = new Timer(200, new ActionListener() {
            boolean visible = true;
            int count = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                btn.setVisible(visible);
                visible = !visible;
                count++;

                if (count > 5) {
                    ((Timer) e.getSource()).stop();
                    btn.setVisible(true);
                }
            }
        });
        timer.start();
    }

    private void verificarSiBarcoHundido() {
        for (Map.Entry<String, List<Point>> entry : barcos.entrySet()) {
            String nombreBarco = entry.getKey();
            List<Point> posicionesBarco = entry.getValue();

            if (posicionesTocadas.containsAll(posicionesBarco)) {
                for (Point p : posicionesBarco) {
                    cambiarIconoCasilla(p.x, p.y, HUNDIDO_ICON_PATH);
                }
                appendMessage("El barco " + nombreBarco + " ha sido hundido.");
            }
        }
    }

    @Override
    public void onMessageReceived(String message) {
        appendMessage("Servidor: " + message);
        if (message.startsWith("#TAB,")) {
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
        } else if (message.startsWith("RIVAL#POS%")) {
            appendMessage("Posiciones de barcos recibidas.");
            procesarPosicionesBarcos(message);
        } else if (message.equals("#INICIO#")) {
            appendMessage("El juego ha comenzado.");
        } else if (message.startsWith("#turno#")) {
            myTurn = true;
            appendMessage("¡Es tu turno! Selecciona una casilla para disparar.");
        } else if (message.equals("#AGUA#") || message.equals("#TOCADO#") || message.equals("#HUNDIDO#")) {
            updateButtonWithShotResult(lastShotRow, lastShotCol, message);
            appendMessage("Resultado del tiro: " + message.replace("#", ""));
        } else if (message.startsWith("#BARCO,")) {
            appendMessage("Barco hundido: " + message);
        } else if (message.startsWith("#FIN#")) {
            appendMessage("Jugador eliminado: " + message);
        } else if (message.startsWith("#GANADOR#")) {
            appendMessage("Ganador: " + message);
            victoryAnimation.startAnimation(this);
        }
    }
}

class SoundManager {
    private Clip clip;
    private FloatControl volumeControl;
    private boolean isMuted = false;

    public SoundManager(String filePath) {
        try {
            // Obtener la URL del recurso
            URL soundURL = getClass().getResource(filePath);

            // Verificar si la URL es válida (archivo no encontrado)
            if (soundURL == null) {
                throw new IllegalArgumentException("Archivo de sonido no encontrado: " + filePath);
            }

            // Cargar el archivo de audio
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            clip = AudioSystem.getClip();
            clip.open(audioStream);

            // Obtener el control de volumen
            volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            // Establecer el volumen al 50% al inicio (ajustado a la mitad entre el mínimo y máximo)
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            volumeControl.setValue(min + (max - min) * 0.5f); // 50% de volumen al inicio

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            // Manejo de excepciones si ocurre algún error al cargar el archivo
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // Excepción personalizada si el archivo no se encuentra
            System.err.println(e.getMessage());
        }
    }

    public void play() {
        if (clip != null) {
            // Reproducir el clip de audio en bucle
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;
        // Establecer el valor de volumen según si está silenciado o no
        volumeControl.setValue(isMuted ? -80.0f : 0.0f);
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setVolume(float volume) {
        if (volumeControl != null) {
            // Establecer el volumen entre el mínimo y máximo
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            volumeControl.setValue(min + (max - min) * volume);
        }
    }
}
