package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
    private int size;
    private List<Ship> ships;
    private boolean[][] hits;
    private boolean[][] misses;

    public Board(int size) {
        this.size = size;
        this.ships = new ArrayList<>();
        this.hits = new boolean[size][size];
        this.misses = new boolean[size][size];
        placeShipsRandomly();
    }

    public int getSize() {
        return size;
    }

    public boolean isShipAt(int row, int col) {
        for (Ship ship : ships) {
            if (ship.contains(new Coordinate(row, col))) {
                return true;
            }
        }
        return false;
    }

    public boolean isHitAt(int row, int col) {
        return hits[row][col];
    }

    public boolean isMissAt(int row, int col) {
        return misses[row][col];
    }

    public void recordHit(int row, int col) {
        hits[row][col] = true;
    }

    public void recordMiss(int row, int col) {
        misses[row][col] = true;
    }

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

    public ShotResult checkShot(Coordinate coord) {
        for (Ship ship : ships) {
            if (!ship.isSunk() && ship.contains(coord)) {
                ship.hit(coord);
                recordHit(coord.getRow(), coord.getCol());
                if (ship.isSunk()) {
                    return new ShotResult(ShotResultType.HUNDIDO, ship.getLength());
                } else {
                    return new ShotResult(ShotResultType.TOCADO, ship.getLength());
                }
            }
        }
        recordMiss(coord.getRow(), coord.getCol());
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

    public String getPositionsMessage(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("#POS,");
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
        sb.append("#");
        return sb.toString();
    }

    private char convertRowToLetter(int row) {
        return (char)('A' + row);
    }
}