package edu.smu.lyle.ultragesture;

/**
 * Created by Arya on 6/12/17.
 */

class Movement {
    public final int index;
    public final int speed;
    public final int angle;

    public Movement() {
        this(-137, -137, -137);
    }

    public Movement(int index, int speed, int angle) {
        this.index = index;
        this.speed = speed;
        this.angle = angle;
    }
}
