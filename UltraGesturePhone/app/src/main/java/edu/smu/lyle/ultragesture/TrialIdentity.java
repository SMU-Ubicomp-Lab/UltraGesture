package edu.smu.lyle.ultragesture;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Arya on 6/13/17.
 */

class TrialIdentity {
    private static final String TRIAL_ID = "TRIAL_ID";
    private final SharedPreferences prefs;
    private final Activity activity;

    TrialIdentity(Activity activity) {
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs == null) {
            Log.wtf(TRIAL_ID, "Why is it nullllllllllllll?");
            throw new AssertionError();
        }
        initializeTrialNumber();
    }

    private void initializeTrialNumber() {
        if (!prefs.contains(TRIAL_ID)) {
            prefs.edit().putInt(TRIAL_ID, 1).apply();
        }
    }

    int getTrialNumber() {
        return prefs.getInt(TRIAL_ID, 0);
    }

    void incrementTrialNumber() {
        prefs.edit().putInt(TRIAL_ID, getTrialNumber() + 1).apply();
    }
}
