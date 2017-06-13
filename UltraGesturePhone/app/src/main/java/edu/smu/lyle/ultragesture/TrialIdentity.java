package edu.smu.lyle.ultragesture;

import android.app.Activity;
import android.content.SharedPreferences;

/**
 * Created by Arya on 6/13/17.
 */

class TrialIdentity {
    private static final String TRIAL_ID = "TRIAL_ID";
    private final SharedPreferences preferences;
    private final Activity activity;

    TrialIdentity(Activity activity) {
        this.activity = activity;
        this.preferences = activity.getPreferences(activity.MODE_PRIVATE);
    }

    int getTrialNumber() {
        return preferences.getInt(TRIAL_ID, 0);
    }

    void incrementTrialNumber() {
        preferences.edit().putInt(TRIAL_ID, preferences.getInt(TRIAL_ID, 0) + 1).apply();
    }
}
