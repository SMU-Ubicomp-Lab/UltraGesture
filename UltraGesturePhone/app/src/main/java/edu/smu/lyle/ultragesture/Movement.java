package edu.smu.lyle.ultragesture;

/**
 * Created by Arya on 6/12/17.
 */

class Movement {
    static final int JUNK = -137;
    int index;
    final int speed;
    final int angle;

    public Movement() {
        this(JUNK, JUNK, JUNK);
    }

    Movement(int index, int speed, int angle) {
        this.index = index;
        this.speed = speed;
        this.angle = angle;
    }

    Movement (int speed, int angle) {
        this.index = JUNK;
        this.speed = speed;
        this.angle = angle;
    }

    static Movement clear() {
        return new Movement();
    }

    static boolean isValid(Movement m) {
        // Nobody cares about the index.
        return m.speed != JUNK && m.angle != JUNK;
    }

    boolean isValid() {
        return Movement.isValid(this);
    }
}
