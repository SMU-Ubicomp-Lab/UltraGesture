package edu.smu.lyle.ultragesture;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.gesture.Sgesture;
import com.samsung.android.sdk.gesture.SgestureHand;
import com.samsung.android.sdk.gesture.SgestureHand.ChangeListener;
import com.samsung.android.sdk.gesture.SgestureHand.Info;

import java.util.Locale;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import edu.samsung.ultragesture.R;

public class UltraGesture extends Activity implements ChangeListener {

    private static final String TAG = "UltraGesture";
    private static UltraGesture ultraGesture = null;
    static UltraGesture getUltraGesture() {
        return ultraGesture;
    }

    @BindView(R.id.start_stop_button)
    Button mStartButton;
    @BindView(R.id.restart_button)
    Button mRestartButton;

    @BindView(R.id.user_id)
    EditText mUserText;
    @BindView(R.id.trial_id)
    TextView mTrialText;

    @BindView(R.id.gesture_title)
    TextView mGestureText;
    @BindView(R.id.gesture_description)
    TextView mDescText;
    @BindView(R.id.countdown)
    TextView mCountdownText;

    @BindView(R.id.gesture_speed)
    TextView mGestureSpeed;
    @BindView(R.id.gesture_angle)
    TextView mGestureAngle;

    @BindString(R.string.gesture_name_generic)
    String GESTURE_NAME_GENERIC;
    @BindString(R.string.gesture_desc_generic)
    String GESTURE_DESCRIPTION_GENERIC;
    @BindString(R.string.go_string)
    String GO_STRING;

    private TrialThread mTrialThread;

    Condition condition;

    private SgestureHand mSGestureHand = null;

    Storage mStorage = new Storage();
    TrialIdentity mIdentity;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            //Get the current snapshot
            CountdownStatus snapshot = (CountdownStatus) inputMessage.obj;

            //Display info
            mGestureText.setText(snapshot.currentGesture.getName());
            mDescText.setText(snapshot.currentGesture.getDesc());

            if (snapshot.countdownTime > 0) {
                mCountdownText.setText(Integer.toString((int) Math.ceil(snapshot.countdownTime / 1000)));
            } else if (snapshot.countdownTime == 0) {
                mCountdownText.setText(GO_STRING);
            } else if (snapshot.countdownTime == -1) {
                if (snapshot.type == MessageType.FAILURE)
                    mCountdownText.setText("Try again.");
                else
                    mCountdownText.setText(R.string.done_string);
            }

            //Check doneWithAllGestures
            if (snapshot.type == MessageType.ALL_DONE) {
                stopTest(true);
            }
        }
    };

    Handler getThreadHandler() {
        return mHandler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ultra_gesture);
        ButterKnife.bind(this);
        ultraGesture = this;

        condition = Condition.INACTIVE;

        Permissions.verifyAllPermissions(this);

        mIdentity = new TrialIdentity(this);
        //Set the volume
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, maxVol);

        //Create gesture
        Sgesture gesture = new Sgesture();
        try {
            gesture.initialize(this);
        } catch (SsdkUnsupportedException e) {
            Log.e(TAG, "Couldn't load Samsung SGesture", e);
            throw new RuntimeException(e);
        }

        //Create gesture hand
        mSGestureHand = new SgestureHand(Looper.getMainLooper(), gesture);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Stop the test on pause
        stopTest(false);
    }

    private void updateUI() {
        switch (condition) {
            case ACTIVE:
                mStartButton.setText("pause");
                mStartButton.setBackgroundColor(ContextCompat.getColor(this, R.color.g_yellow));
                mRestartButton.setEnabled(false);
                mUserText.setEnabled(false);
                break;
            case INACTIVE:
                mStartButton.setText("start");
                mStartButton.setBackgroundColor(ContextCompat.getColor(this, R.color.g_blue));
                mRestartButton.setEnabled(false);
                mUserText.setEnabled(true);
                break;
            case PAUSED:
                mStartButton.setText("resume");
                mStartButton.setBackgroundColor(ContextCompat.getColor(this, R.color.g_blue));
                mRestartButton.setEnabled(true);
                mUserText.setEnabled(false);
                break;
        }
    }

	/*
	 * Test controls
	 */

    private void startTest() {
        //Update state
        condition = Condition.ACTIVE;
        updateUI();

        //Start gesture listener
        mSGestureHand.start(0, this);

        //Create thread
        int trialID = mIdentity.getTrialNumber();
        mTrialThread = new TrialThread(trialID);
        mTrialText.setText(String.format("Trial number: %d", trialID));
        mTrialThread.start();
    }

    private void resumeTest() {
        //Update state
        condition = Condition.ACTIVE;
        updateUI();
    }

    private void pauseTest() {
        //Update state
        condition = Condition.PAUSED;
        updateUI();
    }

    private void stopTest(boolean save) {
        //Update state
        condition = Condition.INACTIVE;
        updateUI();

        //Stop listening to gestures
        try {
            mSGestureHand.stop();
        } catch (IllegalStateException e) {
            // Happens when .start() wasn't called.
            // Do nothing. Just let it slide.
        }

        //Null the test thread
        mTrialThread = null;

        //Check save
        if (save) {
            //Increment test to next
            mIdentity.incrementTrialNumber();

            mGestureText.setText("Finished!");
            mDescText.setText("Thank you for participating.");
        } else {
            mGestureText.setText(GESTURE_NAME_GENERIC);
            mDescText.setText(GESTURE_DESCRIPTION_GENERIC);
            mCountdownText.setText(GO_STRING);
        }
    }

    @OnClick(R.id.start_stop_button)
    void start_or_stop_or_pause() {
        switch (condition) {
            case INACTIVE:
                startTest();
                break;
            case PAUSED:
                resumeTest();
                break;
            case ACTIVE:
                pauseTest();
                break;
        }
    }

    @OnClick(R.id.restart_button)
    void restart_button() {
        stopTest(false);
    }

    @OnEditorAction(R.id.user_id)
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE &&
                TextUtils.isEmpty(mUserText.getText().toString())) {
            mUserText.setText("0");
        }
        return false;
    }

    @OnClick(R.id.oops)
    public void oopsPressed() {
        Log.i(TAG, "Oops! Didn't like that input.");
        // TODO: Add oops file-deletion and rollback behavior.
    }

    @Override
    public void onChanged(/*Gesture*/Info info) {
        //Set the gesture data here

        Movement m = new Movement(info.getSpeed(), info.getAngle());
        mTrialThread.mLastMovement = m;

        Locale locale = Locale.getDefault();
        if (m.isValid()) {
            mGestureSpeed.setText(String.format(locale, "Gesture Speed: %d", m.speed));
            mGestureAngle.setText(String.format(locale, "Gesture Angle: %d", m.angle));
        }
    }

}