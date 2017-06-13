package edu.smu.lyle.ultragesture;

import android.util.Log;

import java.io.File;

/**
 * Created by Arya on 6/12/17.
 */

class Storage {
    private static final String TAG = "Storage";
    private static final String BASE_FOLDER_PATH = "/storage/emulated/0/ultragesture/outputs/";
    private static final String FILENAME_FORMAT = "%s_%s_%s.gest";
    private final File baseFolder;

    Storage() {
        super();
        baseFolder = new File(BASE_FOLDER_PATH);
        this.init();
    }

    void init() {
        if (!baseFolder.exists()) {
            if (baseFolder.mkdirs())
                Log.d(TAG, "Successfully created directory: " + baseFolder.getName());
            else
                Log.d(TAG, "Failed to create directory: " + baseFolder.getName());
        }
    }

    File getFile(String userID, String trialID, Gesture gesture) {
        String filename = String.format(FILENAME_FORMAT, userID, trialID, gesture.getShortName());
        return new File(baseFolder, filename);
    }
}
