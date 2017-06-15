package edu.smu.lyle.ultragesture;

/**
 * Created by Arya on 6/13/17.
 */

class CountdownStatus {
    final Gesture currentGesture;
    final long countdownTime;
    MessageType type;

    CountdownStatus(Gesture g, long time, MessageType type) {
        currentGesture = g;
        countdownTime = time;
        this.type = type;
    }
}
