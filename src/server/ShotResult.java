package server;

public class ShotResult {
    private ShotResultType result;
    private int shipLength;

    public ShotResult(ShotResultType result, int shipLength) {
        this.result = result;
        this.shipLength = shipLength;
    }

    public ShotResultType getResult() {
        return result;
    }

    public int getShipLength() {
        return shipLength;
    }
}
