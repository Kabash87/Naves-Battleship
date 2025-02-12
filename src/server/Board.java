package server;

import resources.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
    private int size;
    private List<Ship> ships;

    public Board(int size) {
        this.size = size;
        this.ships = new ArrayList<>();
        placeShipsRandomly();
    }

    public int getSize() {
        return size;
    }

    public List<Ship> getShips() {
        return ships;
    }

    // Coloca aleatoriamente: 1 barco de longitud 4, 2 de longitud 3 y 1 de longitud 2
    private void placeShipsRandomly() {
        int[] shipLengths = {4, 3, 3, 2};
        for (int len : shipLengths) {
            boolean placed = false;
            while (!placed) {
                placed = tryPlaceShip(len);
            }
        }
    }

    private boolean tryPlaceShip(int length) {
        Random rand = new Random();
        boolean horizontal = rand.nextBoolean();
        int maxRow = horizontal ? size : size - length;
        int maxCol = horizontal ? size - length : size;
        int row = rand.nextInt(maxRow);
        int col = rand.nextInt(maxCol);

        List<Coordinate> newCoords = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int r = row + (horizontal ? 0 : i);
            int c = col + (horizontal ? i : 0);
            newCoords.add(new Coordinate(r, c));
        }

        // Comprobar que no se superpongan con otros barcos
        for (Ship ship : ships) {
            for (Coordinate coord : ship.getCoordinates()) {
                for (Coordinate newCoord : newCoords) {
                    if (coord.equals(newCoord)) {
                        return false;
                    }
                }
            }
        }

        ships.add(new Ship(length, newCoords));
        return true;
    }

    // Comprueba el tiro recibido y devuelve el resultado (AGUA, TOCADO o HUNDIDO)
    public ShotResult checkShot(Coordinate coord) {
        for (Ship ship : ships) {
            if (!ship.isSunk() && ship.contains(coord)) {
                ship.hit(coord);
                if (ship.isSunk()) {
                    return new ShotResult(ShotResultType.HUNDIDO, ship.getLength());
                } else {
                    return new ShotResult(ShotResultType.TOCADO, ship.getLength());
                }
            }
        }
        return new ShotResult(ShotResultType.AGUA, 0);
    }

    public boolean allShipsSunk() {
        for (Ship ship : ships) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    // Genera la cadena de posiciones de barcos que se enviará al cliente.
    public String getPositionsMessage(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append(Protocol.POSITION_PREFIX);
        boolean firstShip = true;
        for (Ship ship : ships) {
            if (!firstShip) {
                sb.append(",");
            }
            firstShip = false;
            for (Coordinate coord : ship.getCoordinates()) {
                sb.append("(").append(convertRowToLetter(coord.getRow()))
                        .append(",").append(coord.getCol() + 1).append(")");
            }
        }
        sb.append(Protocol.BOARD_SUFFIX);
        return sb.toString();
    }

    private char convertRowToLetter(int row) {
        return (char)('A' + row);
    }
}
