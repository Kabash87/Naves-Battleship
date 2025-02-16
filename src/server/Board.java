// src/server/Board.java
package server;

import resources.Protocol;
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

    public boolean canPlaceShip(Ship ship, int row, int col, boolean horizontal) {
        int length = ship.getLength();
        if (horizontal) {
            if (col + length > size) return false;
        } else {
            if (row + length > size) return false;
        }
        List<Coordinate> newCoords = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int r = row + (horizontal ? 0 : i);
            int c = col + (horizontal ? i : 0);
            newCoords.add(new Coordinate(r, c));
        }
        for (Ship existing : ships) {
            for (Coordinate coord : existing.getCoordinates()) {
                for (Coordinate newCoord : newCoords) {
                    if (coord.equals(newCoord)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void placeShip(Ship ship, int row, int col, boolean horizontal) {
        int length = ship.getLength();
        List<Coordinate> coords = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int r = row + (horizontal ? 0 : i);
            int c = col + (horizontal ? i : 0);
            coords.add(new Coordinate(r, c));
        }
        ship.setCoordinates(coords);
        ships.add(ship);
    }

    /**
     * En modo free‑for‑all, devuelve las posiciones de los barcos del jugador indicado.
     */
    public String getPlayerPositionsMessage(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append(Protocol.POSITION_PREFIX);
        boolean firstShip = true;
        for (Ship ship : ships) {
            if (!ship.getOwner().equals(username)) continue;
            if (!firstShip) {
                sb.append(",");
            }
            firstShip = false;
            for (Coordinate coord : ship.getCoordinates()) {
                sb.append("(")
                        .append(convertRowToLetter(coord.getRow()))
                        .append(",").append(coord.getCol() + 1).append(")");
            }
        }
        sb.append("#");
        return sb.toString();
    }

    /**
     * En modo 1 vs 1, devuelve la posición de todos los barcos del tablero.
     */
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
                sb.append("(")
                        .append(convertRowToLetter(coord.getRow()))
                        .append(",").append(coord.getCol() + 1).append(")");
            }
        }
        sb.append("#");
        return sb.toString();
    }

    private char convertRowToLetter(int row) {
        return (char)('A' + row);
    }

    /**
     * Procesa el disparo en la coordenada indicada.
     * Si se impacta un barco, lo marca como hit y retorna el resultado.
     */
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

    /**
     * Dado una coordenada, retorna el dueño del barco que la contenga.
     * Se utiliza en free‑for‑all para identificar a quién pertenece el barco impactado.
     */
    public String getOwnerOfShipAt(Coordinate coord) {
        for (Ship ship : ships) {
            if (ship.contains(coord)) {
                return ship.getOwner();
            }
        }
        return "";
    }

    public boolean allShipsSunkForPlayer(String username) {
        System.out.println("Verificando barcos de " + username);
        for (Ship ship : ships) {
            if (ship.getOwner().equals(username)) {
                System.out.println("Barco de tamaño " + ship.getLength() + " - Hundido: " + ship.isSunk());
                if (!ship.isSunk()) {
                    return false;
                }
            }
        }
        return true;
    }


    public Ship getShipAt(Coordinate coord) {
        for (Ship ship : ships) {
            if (ship.contains(coord)) {
                return ship;
            }
        }
        return null;
    }

    public boolean allShipsSunk() {
        for (Ship ship : ships) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    public Ship[] getShips() {
        return ships.toArray(new Ship[0]);
    }
}

