package edu.smu.lyle.ultragesture;

import android.app.Activity;
import android.os.OperationCanceledException;
import android.util.Log;

import java.util.Collections;
import java.util.List;

/**
 * Created by Arya on 6/13/17.
 */

public class TrialThread {
    private final String TAG = "TrialThread";
    private final long COUNTDOWN_MILLISECONDS = 3000L;
    Activity activity;
    FrequencyEmitter emitter;
    private final List<Gesture> mGestures;

    TrialThread(Activity activity) {
        this.activity = activity;
        mGestures = Gesture.getGestures(activity);
        Collections.shuffle(mGestures);

        emitter = new FrequencyEmitter();
    }

    void run() {
        try {
            discoverGestures();
        }
        catch (OperationCanceledException e) {
            Log.i(TAG, "Trial canceled.", e);
            // TODO: Update UI
        }
    }

    void discoverGestures() {

    }
}
