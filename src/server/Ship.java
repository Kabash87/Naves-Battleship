package server;

import java.util.List;
import java.util.ArrayList;

public class Ship {
    private int length;
    private List<Coordinate> coordinates;
    private List<Coordinate> hits;

    public Ship(int length, List<Coordinate> coordinates) {
        this.length = length;
        this.coordinates = coordinates;
        this.hits = new ArrayList<>();
    }

    public int getLength() {
        return length;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public boolean contains(Coordinate coord) {
        for (Coordinate c : coordinates) {
            if (c.equals(coord)) {
                return true;
            }
        }
        return false;
    }

    public void hit(Coordinate coord) {
        if (contains(coord) && !hits.contains(coord)) {
            hits.add(coord);
        }
    }

    public boolean isSunk() {
        return hits.size() >= length;
    }
}
