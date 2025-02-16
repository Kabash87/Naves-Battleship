// src/server/Ship.java
package server;

import java.util.List;
import java.util.ArrayList;

public class Ship {
    private int length;
    private List<Coordinate> coordinates;
    private List<Coordinate> hits;
    private String owner;

    /**
     * Constructor usado para colocar barcos en free‑for‑all.
     * Se le asigna el tamaño y el username del propietario.
     */
    public Ship(int length, String owner) {
        this.length = length;
        this.owner = owner;
        this.coordinates = new ArrayList<>();
        this.hits = new ArrayList<>();
    }

    /**
     * Constructor alternativo (por ejemplo, para modo 1 vs 1).
     */
    public Ship(int length, List<Coordinate> coordinates) {
        this.length = length;
        this.coordinates = coordinates;
        this.hits = new ArrayList<>();
        this.owner = "";
    }

    public int getLength() {
        return length;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Coordinate> coords) {
        this.coordinates = coords;
    }

    public String getOwner() {
        return owner;
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
            System.out.println("Impacto agregado en " + coord + " | Total impactos: " + hits.size());
        } else {
            System.out.println("Fallo al registrar impacto en " + coord);
        }
    }

    public boolean isSunk() {
        return hits.size() >= length;
    }
}
