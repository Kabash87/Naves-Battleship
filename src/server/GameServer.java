package server;

import resources.Protocol;
import gui.GUIServer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameServer {
    private int port;
    private List<ClientHandler> clients;
    private List<Player> players;
    private ServerSocket serverSocket;
    private boolean accepting = true;
    private GUIServer guiServer;
    private Board commonBoard;

    public GameServer(int port, GUIServer guiServer) {
        this.port = port;
        this.guiServer = guiServer;
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
            guiServer.onMessageReceived("Servidor iniciado en puerto " + port);

            new Thread(() -> {
                try {
                    while (accepting) {
                        Socket socket = serverSocket.accept();
                        System.out.println("Nueva conexión: " + socket.getInetAddress());

                        BufferedReaderWrapper reader = new BufferedReaderWrapper(socket);
                        PrintWriterWrapper writer = new PrintWriterWrapper(socket);

                        String regMsg = reader.readLine();
                        if (regMsg != null && regMsg.startsWith(Protocol.REGISTRATION)) {
                            System.out.println(regMsg);
                            String[] parts = regMsg.split("#");
                            String username = "Jugador" + (players.size() + 1);
                            if (parts.length > 2) {
                                username = parts[2].trim();
                                System.out.println("Nombre personalizado: " + username);
                            }
                            writer.println(Protocol.REG_OK + username);
                            Player player = new Player(username, socket, writer.getPrintWriter(), null);
                            players.add(player);
                            ClientHandler handler = new ClientHandler(socket, player);
                            clients.add(handler);
                            handler.start();
                            System.out.println("Registrado: " + username);
                        } else {
                            socket.close();
                        }
                    }
                } catch (SocketException se) {
                    if (!accepting) {
                        System.out.println("Servidor: Socket cerrado correctamente, fin de registro.");
                    } else {
                        se.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            while (players.size() < 2) {
                Thread.sleep(500);
            }

            System.out.println("Se han registrado al menos 2 jugadores. Iniciando periodo de registro de 10 segundos.");
            guiServer.onMessageReceived("Se han registrado al menos 2 jugadores. Iniciando periodo de registro de 10 segundos.");
            Thread.sleep(10000);
            accepting = false;
            serverSocket.close();
            System.out.println("Fin del periodo de registro. Total jugadores: " + players.size());
            guiServer.onMessageReceived("Fin del periodo de registro. Total jugadores: " + players.size());

            int boardSize = calculateBoardSize(players.size());

            if (players.size() == 2) {
                for (Player player : players) {
                    Board board = new Board(boardSize);
                    player.setBoard(board);
                    placeShipsForPlayer(board, player);
                    player.getOut().println(Protocol.BOARD_PREFIX + boardSize + Protocol.BOARD_SUFFIX);
                    String posMsg = board.getPositionsMessage(player.getUsername());
                    player.getOut().println(posMsg);
                }
                for (int i = 0; i < players.size(); i++) {
                    Player sender = players.get(i);
                    Player receiver = players.get((i - 1 + players.size()) % players.size());
                    String senderPosMsg = sender.getBoard().getPositionsMessage(sender.getUsername());
                    receiver.getOut().println(Protocol.POSITION_RIVAL + senderPosMsg);
                }
            } else {
                commonBoard = new Board(boardSize);
                for (Player player : players) {
                    placeShipsForPlayer(commonBoard, player);

                    player.setBoard(commonBoard);
                    String posMsg = commonBoard.getPlayerPositionsMessage(player.getUsername());
                    player.getOut().println(posMsg);
                }
                broadcastMessage(Protocol.BOARD_PREFIX + boardSize + Protocol.BOARD_SUFFIX);
            }

            broadcastMessage(Protocol.START_GAME);

            if (commonBoard != null) {
                runGameLoop(commonBoard);
            } else {
                runGameLoop(null);
            }

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
            return 9 + numPlayers;
        }
    }

    private void broadcastMessage(String msg) {
        for (Player p : players) {
            if (p.getOut() != null) {
                p.getOut().println(msg);
            }
        }
        guiServer.onMessageReceived(msg);
    }

    /**
     * Coloca aleatoriamente los barcos para el jugador en el tablero dado.
     * Cada jugador tiene: un barco de tamaño 4, dos de tamaño 3 y uno de tamaño 2.
     */
    private void placeShipsForPlayer(Board board, Player player) {
        int[] shipSizes = {4, 3, 3, 2};
        Random rand = new Random();

        for (int size : shipSizes) {
            boolean placed = false;
            while (!placed) {
                boolean horizontal = rand.nextBoolean();
                int maxRow = board.getSize() - (horizontal ? 0 : size);
                int maxCol = board.getSize() - (horizontal ? size : 0);

                int row = rand.nextInt(maxRow);
                int col = rand.nextInt(maxCol);

                // Se crea un barco asignándole el tamaño y el identificador del jugador
                Ship ship = new Ship(size, player.getUsername());
                // Verifica si se puede colocar el barco en la posición elegida
                if (board.canPlaceShip(ship, row, col, horizontal)) {
                    board.placeShip(ship, row, col, horizontal);
                    placed = true;
                }
            }
        }
    }

    /**
     * Bucle principal del juego.
     * Si commonBoard es distinto de null, estamos en modo free‑for‑all;
     * en caso contrario, en modo 1 vs 1.
     */
    private void runGameLoop(Board commonBoard) {
        int currentIndex = 0;

        while (activePlayers() > 1) {
            while (!players.get(currentIndex).isActive()) {
                currentIndex = (currentIndex + 1) % players.size();
            }

            if (activePlayers() <= 1) {
                break;
            }

            Player attacker = players.get(currentIndex);
            int currentBoardSize = (commonBoard != null) ? commonBoard.getSize() : attacker.getBoard().getSize();

            attacker.getOut().println(Protocol.TURN_PREFIX + "30");
            for (Player p : players) {
                if (!p.equals(attacker) && p.isActive()) {
                    p.getOut().println("Esperando al movimiento de " + attacker.getUsername() + "...");
                }
            }

            ClientHandler handler = getClientHandler(attacker);
            String shotMsg = handler.waitForShot(30);
            if (shotMsg == null) {
                System.out.println(attacker.getUsername() + " no respondió a tiempo. Turno perdido.");
                guiServer.onMessageReceived(attacker.getUsername() + " no respondió a tiempo. Turno perdido.");
                currentIndex = (currentIndex + 1) % players.size();
                continue;
            }

            Coordinate shotCoord = parseShot(shotMsg, currentBoardSize);
            if (shotCoord == null) {
                attacker.getOut().println("Tiro inválido.");
                currentIndex = (currentIndex + 1) % players.size();
                continue;
            }

            ShotResult result;
            Player target = getNextActivePlayer(currentIndex);
            if (target == null) break;

            result = target.getBoard().checkShot(shotCoord);

            if (result.getResult() == ShotResultType.AGUA) {
                attacker.getOut().println(Protocol.AGUA);
                guiServer.onMessageReceived(attacker.getUsername() + " ha obtenido: AGUA");
            } else if (result.getResult() == ShotResultType.TOCADO) {
                attacker.getOut().println(Protocol.TOCADO);
                guiServer.onMessageReceived(attacker.getUsername() + " ha obtenido: TOCADO");
            } else if (result.getResult() == ShotResultType.HUNDIDO) {
                attacker.getOut().println(Protocol.HUNDIDO);
                guiServer.onMessageReceived(attacker.getUsername() + " ha obtenido: HUNDIDO");

                Ship sunkShip = target.getBoard().getShipAt(shotCoord);
                if (sunkShip != null) {
                    String posMessage = buildSunkShipPositionMessage(sunkShip, players.size());
                    broadcastMessage(posMessage);
                }

                System.out.println(target.getBoard().allShipsSunkForPlayer(target.getUsername()));
                if (target.getBoard().allShipsSunkForPlayer(target.getUsername())) {
                    target.setActive(false);
                    broadcastMessage(Protocol.FIN + target.getUsername() + Protocol.BOARD_SUFFIX);

                    if (activePlayers() == 1) {
                        break;
                    }
                }

                printBarcos(target);

                for (Player p : players) {
                    System.out.println("Jugador: " + p.getUsername() + " Activo: " + p.isActive());
                }
            }

            guiServer.updateBoard(target.getBoard(), attacker.getBoard(), target.getUsername());

            do {
                currentIndex = (currentIndex + 1) % players.size();
            } while (!players.get(currentIndex).isActive());

        }

        Player winner = null;
        for (Player p : players) {
            if (p.isActive()) {
                winner = p;
                break;
            }
        }

        if (winner != null) {
            broadcastMessage(Protocol.GANADOR + winner.getUsername() + Protocol.BOARD_SUFFIX);
            System.out.println("El ganador es: " + winner.getUsername());
            guiServer.onMessageReceived("El ganador es: " + winner.getUsername());
        }

        System.out.println("Juego finalizado.");
        guiServer.onMessageReceived("Juego finalizado.");
    }
    public void printBarcos(Player target) {
        for (Ship ship : target.getBoard().getShips()) {
            System.out.println("Barco de tamaño: " + ship.getLength() +
                    " | Dueño: " + ship.getOwner());
        }
    }

    private int activePlayers() {
        int count = 0;
        for (Player p : players) {
            if (p.isActive()) count++;
        }
        return count;
    }

    private String buildSunkShipPositionMessage(Ship ship, int activePlayerCount) {
        if (activePlayerCount >= 3) {
            StringBuilder sb = new StringBuilder();
            sb.append(Protocol.POSITION_BARCO)
                    .append(",")
                    .append(ship.getLength())
                    .append(",")
                    .append(ship.getOwner())
                    .append(Protocol.BOARD_SUFFIX);
            for (Coordinate coord : ship.getCoordinates()) {
                sb.append("(")
                        .append(convertRowToLetter(coord.getRow()))
                        .append(",")
                        .append(coord.getCol() + 1)
                        .append(")");
            }
            return sb.toString();
        }
        return "";
    }

    private String convertRowToLetter(int row) {
        return Character.toString((char)('A' + row));
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

    /**
     * Parsea el mensaje de tiro con formato "#TIRO(F,C)#" y devuelve un objeto Coordinate.
     */
    private Coordinate parseShot(String shotMsg, int boardSize) {
        try {
            int start = shotMsg.indexOf('(');
            int end = shotMsg.indexOf(')');
            if (start == -1 || end == -1) return null;
            String content = shotMsg.substring(start + 1, end);
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