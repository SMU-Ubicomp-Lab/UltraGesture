package edu.smu.lyle.ultragesture;

import android.util.Log;

import java.io.File;

/**
 * Created by Arya on 6/12/17.
 */

class Storage {
    private String TAG = "Storage";
    private String BASE_FOLDER_PATH = "/storage/emulated/0/ultragesture/outputs/";
    private String FILENAME_FORMAT = "%s_%s_%s.gest";
    private File baseFolder;

    Storage() {
        super();
        baseFolder = new File(BASE_FOLDER_PATH);
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
