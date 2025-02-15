// src/server/GameServer.java
package server;

import resources.Protocol;
import gui.GUIServer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
    private int port;
    private List<ClientHandler> clients;
    private List<Player> players;
    private ServerSocket serverSocket;
    private boolean accepting = true;
    private GUIServer guiServer; // Reference to GUIServer

    public GameServer(int port, GUIServer guiServer) {
        this.port = port;
        this.guiServer = guiServer; // Initialize GUIServer reference
        clients = new ArrayList<>();
        players = new ArrayList<>();
    }

    public void setGuiServer(GUIServer guiServer) {
        this.guiServer = guiServer;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Servidor iniciado en puerto " + port);
            guiServer.onMessageReceived("Servidor iniciado en puerto " + port); // Update GUI

            // Thread para aceptar conexiones
            new Thread(() -> {
                try {
                    while (accepting) {
                        Socket socket = serverSocket.accept();
                        System.out.println("Nueva conexión: " + socket.getInetAddress());
                        guiServer.onMessageReceived("Nueva conexión: " + socket.getInetAddress()); // Update GUI

                        BufferedReaderWrapper reader = new BufferedReaderWrapper(socket);
                        PrintWriterWrapper writer = new PrintWriterWrapper(socket);

                        String regMsg = reader.readLine();
                        if (regMsg != null && regMsg.equals(Protocol.REGISTRATION)) {
                            writer.println(Protocol.REG_OK);
                            // Asigna nombre automáticamente (Jugador1, Jugador2, …)
                            String username = "Jugador" + (players.size() + 1);
                            Player player = new Player(username, socket, writer.getPrintWriter(), null);
                            players.add(player);
                            ClientHandler handler = new ClientHandler(socket, player);
                            clients.add(handler);
                            handler.start();
                            System.out.println("Registrado: " + username);
                            guiServer.onMessageReceived("Registrado: " + username); // Update GUI
                        } else {
                            socket.close();
                        }
                    }
                } catch (SocketException se) {
                    if (!accepting) {
                        System.out.println("Servidor: Socket cerrado correctamente, fin de registro.");
                        guiServer.onMessageReceived("Servidor: Socket cerrado correctamente, fin de registro."); // Update GUI
                    } else {
                        se.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Espera a que se conecten al menos dos jugadores
            while (players.size() < 2) {
                Thread.sleep(500);
            }

            System.out.println("Se han registrado al menos 2 jugadores. Iniciando periodo de registro de 30 segundos.");
            guiServer.onMessageReceived("Se han registrado al menos 2 jugadores. Iniciando periodo de registro de 10 segundos."); // Update GUI
            Thread.sleep(10000);  // 10 segundos para más registros
            accepting = false;
            serverSocket.close();
            System.out.println("Fin del periodo de registro. Total jugadores: " + players.size());
            guiServer.onMessageReceived("Fin del periodo de registro. Total jugadores: " + players.size()); // Update GUI

            // Calcula el tamaño del tablero según el número de jugadores
            int boardSize = calculateBoardSize(players.size());

            // Asigna un tablero a cada jugador, y envía al cliente el mensaje de tablero y posiciones
            for (Player player : players) {
                Board board = new Board(boardSize);
                Board ownBoard = new Board(boardSize);

                player.setBoard(board);
                // Envía mensaje: "#TAB,n#"
                player.getOut().println(Protocol.BOARD_PREFIX + boardSize + Protocol.BOARD_SUFFIX);
                // Envía posiciones de barcos
                String posMsg = board.getPositionsMessage(player.getUsername());
                player.getOut().println(posMsg);
                guiServer.updateBoard(board, ownBoard, player.getUsername()); // Update GUI
            }

            // Notifica el inicio de partida
            broadcastMessage(Protocol.START_GAME);

            // Comienza la partida (se asume un juego free-for-all en turno rotativo)
            runGameLoop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int calculateBoardSize(int numPlayers) {
        if (numPlayers == 2) {
            return 10;
        } else if (numPlayers == 3) {
            return 12;
        } else {
            return 9 + numPlayers; // Por ejemplo, 4 jugadores -> 13, 5 -> 14, etc.
        }
    }

    private void broadcastMessage(String msg) {
        for (Player p : players) {
            if (p.getOut() != null) {
                p.getOut().println(msg);
            }
        }
        guiServer.onMessageReceived(msg); // Update GUI
    }

    private void runGameLoop() {
        int currentIndex = 0;
        while (activePlayers() > 1) {
            // El jugador que ataca
            Player attacker = players.get(currentIndex);
            if (!attacker.isActive()) {
                currentIndex = (currentIndex + 1) % players.size();
                continue;
            }
            // Selecciona como objetivo al siguiente jugador activo
            Player target = getNextActivePlayer(currentIndex);
            if (target == null) break;

            // Envía mensaje de turno al atacante, indicando un tiempo (por ejemplo, 30 segundos)
            String turnMessage = Protocol.TURN_PREFIX + "30";
            attacker.getOut().println(turnMessage);

            // Notifica a los demás jugadores que están esperando
            for (Player player : players) {
                if (!player.equals(attacker) && player.isActive()) {
                    player.getOut().println("Esperando al movimiento del " + attacker.getUsername()+ "...");
                }
            }

            // Obtiene el ClientHandler correspondiente
            ClientHandler handler = getClientHandler(attacker);
            String shotMsg = handler.waitForShot(30);
            if (shotMsg == null) {
                System.out.println(attacker.getUsername() + " no respondió a tiempo. Turno perdido.");
                guiServer.onMessageReceived(attacker.getUsername() + " no respondió a tiempo. Turno perdido."); // Update GUI
                currentIndex = (currentIndex + 1) % players.size();
                continue;
            }

            // Se espera que el mensaje tenga el formato: "#TIRO(F,C)#"
            Coordinate shotCoord = parseShot(shotMsg, target.getBoard().getSize());
            if (shotCoord == null) {
                attacker.getOut().println("Tiro inválido.");
                currentIndex = (currentIndex + 1) % players.size();
                continue;
            }

            // Procesa el tiro sobre el tablero del jugador objetivo
            ShotResult result = target.getBoard().checkShot(shotCoord);
            if (result.getResult() == ShotResultType.AGUA) {
                attacker.getOut().println(Protocol.AGUA);
                guiServer.onMessageReceived(attacker.getUsername() + " ha obtenido: AGUA"); // Update GUI
            } else if (result.getResult() == ShotResultType.TOCADO) {
                attacker.getOut().println(Protocol.TOCADO);
                guiServer.onMessageReceived(attacker.getUsername() + " ha obtenido: TOCADO"); // Update GUI
            } else if (result.getResult() == ShotResultType.HUNDIDO) {
                attacker.getOut().println(Protocol.HUNDIDO);
                guiServer.onMessageReceived(attacker.getUsername() + " ha obtenido: HUNDIDO"); // Update GUI
                // Notifica a todos el hundimiento del barco: "#BARCO,TAMAÑO,USUARIO#"
                String sunkMsg = Protocol.BARCO + "," + result.getShipLength() + "," + target.getUsername() + Protocol.BOARD_SUFFIX;
                broadcastMessage(sunkMsg);
            }

            // Si el objetivo ha perdido todos sus barcos se le marca como eliminado y se notifica
            if (target.getBoard().allShipsSunk()) {
                target.setActive(false);
                String finMsg = Protocol.FIN + target.getUsername() + Protocol.BOARD_SUFFIX;
                broadcastMessage(finMsg);
            }

            guiServer.updateBoard(target.getBoard(), attacker.getBoard(), target.getUsername()); // Update GUI

            currentIndex = (currentIndex + 1) % players.size();
        }

        // Cuando solo quede un jugador activo se notifica al ganador
        Player winner = null;
        for (Player p : players) {
            if (p.isActive()) {
                winner = p;
                break;
            }
        }
        if (winner != null) {
            String winMsg = Protocol.GANADOR + winner.getUsername() + Protocol.BOARD_SUFFIX;
            broadcastMessage(winMsg);
        }
        System.out.println("Juego finalizado.");
        guiServer.onMessageReceived("Juego finalizado."); // Update GUI
    }

    private int activePlayers() {
        int count = 0;
        for (Player p : players) {
            if (p.isActive()) count++;
        }
        return count;
    }

    private Player getNextActivePlayer(int currentIndex) {
        int count = players.size();
        for (int i = 1; i < count; i++) {
            int nextIndex = (currentIndex + i) % players.size();
            if (players.get(nextIndex).isActive()) {
                return players.get(nextIndex);
            }
        }
        return null;
    }

    private ClientHandler getClientHandler(Player player) {
        for (ClientHandler handler : clients) {
            if (handler.getPlayer().getUsername().equals(player.getUsername())) {
                return handler;
            }
        }
        return null;
    }

    // Parsea el mensaje de tiro "#TIRO(F,C)#" y devuelve el objeto Coordinate
    private Coordinate parseShot(String shotMsg, int boardSize) {
        try {
            int start = shotMsg.indexOf('(');
            int end = shotMsg.indexOf(')');
            if (start == -1 || end == -1) return null;
            String content = shotMsg.substring(start+1, end);
            String[] parts = content.split(",");
            if (parts.length != 2) return null;
            char letter = parts[0].trim().charAt(0);
            int row = letter - 'A';
            int col = Integer.parseInt(parts[1].trim()) - 1;
            if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) {
                return null;
            }
            return new Coordinate(row, col);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}