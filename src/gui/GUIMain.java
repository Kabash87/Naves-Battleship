package gui;

import client.Client;
import client.ClientListener;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
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
    private String nombre;
    private JTextArea textArea;
    private JPanel boardPanel;
    private int boardSize = 10;
    private boolean myTurn = false;
    private int lastShotRow = -1, lastShotCol = -1;
    private String posicionesBarcos = null;
    private SoundManager soundManager;
    private JButton muteButton;
    private JSlider volumeSlider;
    private Map<String, List<Point>> barcos = new HashMap<>();
    private Set<Point> posicionesTocadas = new HashSet<>();
    private static final String AGUA_ICON_PATH = "/resources/agua.png";
    private static final String TOCADO_ICON_PATH = "/resources/tocado.png";
    private static final String HUNDIDO_ICON_PATH = "/resources/hundido.png";
    private static final String TUBARCO_ICON_PATH = "/resources/tubarco.png";

    VictoryAnimation victoryAnimation = new VictoryAnimation();
    DefeatAnimation defeatAnimation = new DefeatAnimation();

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
        
        getLayeredPane().add(defeatAnimation, JLayeredPane.POPUP_LAYER);
        defeatAnimation.setSize(200, 100);
        defeatAnimation.setVisible(false);

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

        JPanel topPanel = new JPanel(new FlowLayout());

        muteButton = new JButton("🔊");
        muteButton.addActionListener(e -> {
            soundManager.toggleMute();
            muteButton.setText(soundManager.isMuted() ? "🔇" : "🔊");
        });
        topPanel.add(muteButton);

        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.addChangeListener(e -> {
            float volume = volumeSlider.getValue() / 100f;
            soundManager.setVolume(volume);
        });
        topPanel.add(new JLabel("Volumen:"));
        topPanel.add(volumeSlider);

        JButton toggleThemeButton = new JButton("Modo Oscuro");
        toggleThemeButton.addActionListener(e -> {
            toggleTheme();
            toggleThemeButton.setText(darkMode ? "Modo Claro" : "Modo Oscuro");
        });
        topPanel.add(toggleThemeButton);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        Color bg = darkMode ? Color.DARK_GRAY : Color.WHITE;
        Color fg = darkMode ? Color.WHITE : Color.BLACK;

        getContentPane().setBackground(bg);
        textArea.setBackground(bg);
        textArea.setForeground(fg);
        boardPanel.setBackground(bg);

        for (Component comp : boardPanel.getComponents()) {
            if (!(comp instanceof JButton)) {
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
                btn.setBackground(Color.LIGHT_GRAY);
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
                        String msgCliente = "(" + convertRowToLetter(r) + "," + (c + 1) + ")";
                        appendMessage("Enviando tiro: " + msgCliente);
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
    
    private void actualizarCasillaComoPropia(int row, int col) {
        SwingUtilities.invokeLater(() -> {
            int index = row * boardSize + col;
            Component comp = boardPanel.getComponent(index);
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                int btnSize = Math.min(btn.getWidth(), btn.getHeight());
                ImageIcon icon = loadAndScaleIcon(TUBARCO_ICON_PATH, btnSize, btnSize);
                btn.setIcon(icon);
                btn.setDisabledIcon(icon);
                btn.setEnabled(false);
            }
        });
    }
    
    private void procesarPosicionesPropias(String mensaje) {
        mensaje = mensaje.replace("#POS%", "").replace("#", "");

        Pattern coordinatePattern = Pattern.compile("\\((\\w),(\\d+)\\)");
        Matcher matcher = coordinatePattern.matcher(mensaje);
        
        while (matcher.find()) {
            char filaChar = matcher.group(1).charAt(0);
            int fila = filaChar - 'A';
            int columna = Integer.parseInt(matcher.group(2)) - 1;
            actualizarCasillaComoPropia(fila, columna);
        }
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
    
    private void convertirPosiciones(String mensaje) {
        Pattern shipPattern = Pattern.compile("\\((\\w),(\\d+)\\)");
        Matcher coordMatcher = shipPattern.matcher(mensaje);

        while (coordMatcher.find()) {
            char filaChar = coordMatcher.group(1).charAt(0);
            int fila = filaChar - 'A';
            int columna = Integer.parseInt(coordMatcher.group(2)) - 1;
            updateButtonWithShotResult(fila, columna, "#HUNDIDO#");
        }
    }

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("#TAB,")) {
        	try {
                int start = message.indexOf(",") + 1;
                int end = message.indexOf("#", start);
                int size = Integer.parseInt(message.substring(start, end));
                appendMessage("Tableto de " + size + " x " + size);
                boardSize = size;
                updateBoardSize(size);

                if (posicionesBarcos != null && boardSize > 10) {
                    procesarPosicionesPropias(posicionesBarcos);
                    appendMessage("Tus barcos se han marcado en verde.");
                    posicionesBarcos = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (message.startsWith("#POS%")) {
        	posicionesBarcos = message;
        	String[] data = message.split("%");
            appendMessage("Tus barcos: " + data[1].substring(0, data[1].length() - 1));
        } else if (message.startsWith("#REG_OK#")) {
        	String[] data = message.split("#");
        	nombre = data[2].trim();
        	appendMessage("Jugador " + nombre + " registrado");
        } else if (message.startsWith("RIVAL#POS%")) {
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
        } else if (message.startsWith("#POSBARCO%")) {
        	message = message.replace("#POSBARCO%", "").replace("#", "");
            convertirPosiciones(message);
        } else if (message.startsWith("#FIN#")) {
            String[] data = message.split("#");
            if (data[2].trim().equals(nombre)) {
                defeatAnimation.startAnimation(this);
                appendMessage("has sido eliminado");
            } else
            	appendMessage("El jugador " + data[2].trim() + " ha sido eliminado");
        } else if (message.startsWith("#GANADOR#")) {
            String[] data = message.split("#");
            if (data[2].trim().equals(nombre)) {
            	victoryAnimation.startAnimation(this);
                appendMessage("¡¡¡HAS GANADO!!!");
            } else
                appendMessage("Ganador: " + data[2].trim());
        }	
    }
}

class SoundManager {
    private Clip clip;
    private FloatControl volumeControl;
    private boolean isMuted = false;
    private float previousVolume = 0.5f;

    public SoundManager(String filePath) {
        try {
            URL soundURL = getClass().getResource(filePath);

            if (soundURL == null) {
                throw new IllegalArgumentException("Archivo de sonido no encontrado: " + filePath);
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            clip = AudioSystem.getClip();
            clip.open(audioStream);

            volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            previousVolume = min + (max - min) * 0.5f;
            volumeControl.setValue(previousVolume);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    public void play() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            previousVolume = volumeControl.getValue();
            volumeControl.setValue(-80.0f);
        } else {
            volumeControl.setValue(previousVolume);
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setVolume(float volume) {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            volumeControl.setValue(min + (max - min) * volume);
        }
    }
}