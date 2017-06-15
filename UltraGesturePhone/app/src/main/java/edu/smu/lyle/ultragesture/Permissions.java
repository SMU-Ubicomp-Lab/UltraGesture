package edu.smu.lyle.ultragesture;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

/**
 * Created by Arya on 6/13/17.
 */

class Permissions {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int RECORD_AUDIO = 2;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
    };

    private static void verifyPermissions(Activity activity, String action, int requestCode) {
        int permitted = ActivityCompat.checkSelfPermission(activity, action);

        if (permitted != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission, so prompt the user.
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    requestCode
            );
        }
    }

    private static void verifyStoragePermissions(Activity activity) {
        verifyPermissions(activity, PERMISSIONS_STORAGE[1], REQUEST_EXTERNAL_STORAGE);
    }

    private static void verifyAudioPermissions(Activity activity) {
        verifyPermissions(activity, PERMISSIONS_STORAGE[2], RECORD_AUDIO);
    }

    static void verifyAllPermissions(Activity activity) {
        verifyStoragePermissions(activity);
        verifyAudioPermissions(activity);
    }

}
