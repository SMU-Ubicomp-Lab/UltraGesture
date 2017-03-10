package edu.smu.lyle.ultragesture;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.samsung.android.sdk.gesture.Sgesture;
import com.samsung.android.sdk.gesture.SgestureHand;
import com.samsung.android.sdk.gesture.SgestureHand.ChangeListener;
import com.samsung.android.sdk.gesture.SgestureHand.Info;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.samsung.ultragesture.R;

enum Condition {
    INACTIVE,
    ACTIVE,
    PAUSED,
}

public class UltraGesture extends Activity implements ChangeListener {

    private static final String TAG = "UltraGesture";
    private static final String TEST_ID = "TEST_ID";

    @BindView(R.id.start_stop_button) Button mStartButton;
    @BindView(R.id.restart_button) Button mRestartButton;

    @BindView(R.id.gesture_title) TextView mGestureText;
    @BindView(R.id.gesture_description) TextView mDescText;
    @BindView(R.id.countdown) TextView mCountdownText;

    @BindString(R.string.gesture_name_generic) String GESTURE_NAME_GENERIC;
    @BindString(R.string.gesture_desc_generic) String GESTURE_DESCRIPTION_GENERIC;
    @BindString(R.string.go_string) String GO_STRING;

    private TestThread mTestThread;

    private Condition condition = Condition.INACTIVE;

    private SgestureHand mSGestureHand = null;
    private int mLastSpeed = -1;
    private int mLastAngle = -1;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            //Get the current state
            TestState state = (TestState)inputMessage.obj;

            //Display info
            mGestureText.setText(state.currentGesture.getName());
            mDescText.setText(state.currentGesture.getDesc());

            if(state.countdownTime > 0) {
                mCountdownText.setText(Integer.toString((int)Math.ceil(state.countdownTime / 1000)));
            }
            else if(state.countdownTime == 0) {
                mCountdownText.setText(GO_STRING);
            }
            else if(state.countdownTime == -1) {
                mCountdownText.setText(R.string.done_string);
            }

            //Check done
            if(state.done) {
                stopTest(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ultra_gesture);
        ButterKnife.bind(this);

        //Check output id in shared prefs
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        if(!sp.contains(TEST_ID)) {
            sp.edit().putInt(TEST_ID, 1).apply();
        }

        //Ensure directory exists
        File directory = new File("/storage/emulated/0/ultragesture/outputs/");
        if(!directory.exists()) {
            directory.mkdirs();
        }

        //Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Set the volume
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, maxVol);

        //Create gesture
        Sgesture gesture = new Sgesture();

        try {
            gesture.initialize(this);
        } catch(Exception e) {
            Log.e(TAG, "Couldn't load gesture", e);
        }

        //Create gesture hand
        mSGestureHand = new SgestureHand(Looper.getMainLooper(), gesture);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Stop the test on pause
        pauseTest();
    }

    private void updateUI() {
        switch (condition) {
            case ACTIVE:
                mStartButton.setText("pause");
                mStartButton.setBackgroundColor(getResources().getColor(R.color.g_yellow));
                mRestartButton.setEnabled(false);
                break;
            case INACTIVE:
                mStartButton.setText("start");
                mStartButton.setBackgroundColor(getResources().getColor(R.color.g_blue));
                mRestartButton.setEnabled(false);
                break;
            case PAUSED:
                mStartButton.setText("resume");
                mStartButton.setBackgroundColor(getResources().getColor(R.color.g_blue));
                mRestartButton.setEnabled(true);
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
        mTestThread = new TestThread();
        mTestThread.start();
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
        mSGestureHand.stop();
        mLastSpeed = -1;
        mLastAngle = -1;

        //Null the test thread
        mTestThread = null;

        //Check save
        if(save) {
            //Increment test to next
            SharedPreferences sp = getPreferences(MODE_PRIVATE);
            sp.edit().putInt(TEST_ID, sp.getInt(TEST_ID, 0) + 1).apply();

            mGestureText.setText("Finished!");
            mDescText.setText("Thank you for participating.");
        }
        else {
            mGestureText.setText(GESTURE_NAME_GENERIC);
            mDescText.setText(GESTURE_DESCRIPTION_GENERIC);
            mCountdownText.setText(GO_STRING);
        }
    }

    @OnClick(R.id.start_stop_button) void start_or_stop_or_pause() {
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

    @OnClick(R.id.restart_button) void restart_button() {
        stopTest(false);
    }

    @Override
    public void onChanged(Info info) {
        //Set the gesture data here
        mLastSpeed = info.getSpeed();
        mLastAngle = info.getAngle();
    }

    private class TestThread extends Thread {

        private final long TIME_DELAY = 3000L;
        FrequencyEmitter emitter;
        private final List<Gesture> mGestures;

        TestThread() {
            //Get list of gestures
            mGestures = Gesture.getGestures(UltraGesture.this);
            Collections.shuffle(mGestures);

            emitter = new FrequencyEmitter(16000f);
        }

        @Override
        public void run() {
            //Loop for each gesture
            gloop:for(Gesture gesture : mGestures) {
                //Send initial message
                sendMessage(gesture, TIME_DELAY);

                //Start countdown
                long goalTime = System.currentTimeMillis() + TIME_DELAY;
                long nextGoal = TIME_DELAY - 1000L;

                long curTime;
                boolean lastPaused = false;
                while(nextGoal >= 0) {
                    //Cancel logic
                    if(condition == Condition.INACTIVE) {
                        break gloop;
                    }

                    //Pausing logic
                    if(condition == Condition.PAUSED) {
                        goalTime = System.currentTimeMillis() + TIME_DELAY;
                        nextGoal = TIME_DELAY - 1000L;

                        if(!lastPaused)
                            sendMessage(gesture, TIME_DELAY);

                        lastPaused = true;
                        continue;
                    }
                    else {
                        lastPaused = false;
                    }

                    //Get the current time remaining
                    curTime = goalTime - System.currentTimeMillis();
                    if(curTime < nextGoal) {
                        sendMessage(gesture, nextGoal);
                        nextGoal -= 1000L;
                    }
                }

                //Start emitting frequency
                emitter.start();

                //Generate filename, file, and writer
                SharedPreferences sp = getPreferences(MODE_PRIVATE);
                String filename = sp.getInt(TEST_ID, 0) + "_" + gesture.getShortName();
                File rawFile = new File("/storage/emulated/0/ultragesture/outputs/" + filename + ".gest");

                //Start writing to file
                try {
                    final DataOutputStream mRawWriter = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rawFile)));

                    //Write header
                    mRawWriter.writeByte(0); //Revision
                    mRawWriter.writeInt(AudioPoller.SAMPLE_RATE); //Sample rate
                    mRawWriter.writeInt(16000); //Frequency 1
                    mRawWriter.writeInt(0); //Frequency 2
                    mRawWriter.writeInt(0); //Number of audio samples (will rewrite later)
                    mRawWriter.writeByte(0); //Number of direction samples
                    mRawWriter.writeLong(0); //Length of audio sample (in nanoseconds)

                    //Create the audio recorder
                    int mBufferSize = AudioRecord.getMinBufferSize(AudioPoller.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    AudioRecord mAudioRecord = new AudioRecord(AudioSource.MIC, AudioPoller.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize);
                    mAudioRecord.startRecording();

                    //Reset gesture data
                    ArrayList<Integer> gestures = new ArrayList<>();
                    mLastAngle = mLastSpeed = -1;

                    //Create and start the audio poller
                    short pcmData[] = new short[512];
                    int state;
                    int numSamples = 0;
                    long startTime = System.nanoTime();
                    while(startTime + 1000000000L > System.nanoTime()) {
                        //Read in data
                        state = mAudioRecord.read(pcmData, 0, 512);

                        if(state == AudioRecord.ERROR_INVALID_OPERATION)
                            Log.e(TAG, "Invalid operation");
                        else if(state == AudioRecord.ERROR_BAD_VALUE)
                            Log.e(TAG, "Invalid arguments");
                        else if(state < 512)
                            Log.e(TAG, "Buffer not filled");

                        //Write data to file
                        for(int x = 0; x < state; x++)
                            mRawWriter.writeShort(pcmData[x]);

                        //Increment count
                        numSamples += state;

                        if(mLastSpeed != -1) {
                            Log.v(TAG, "Gesture detected");

                            //Add data to list with timestamp
                            gestures.add(numSamples);
                            gestures.add(mLastSpeed);
                            gestures.add(mLastAngle);

                            //Reset gesture
                            mLastAngle = mLastSpeed = -1;
                        }
                    }

                    //Get the length of the recording
                    long lengthOfRecord = System.nanoTime() - startTime;

                    //Stop recording
                    mAudioRecord.stop();
                    mAudioRecord.release();

                    //Stop emitting signal
                    emitter.stop();

                    //Write footer (sgesture movements)
                    for(int x = 0; x < gestures.size(); x+=3) {
                        mRawWriter.writeInt(gestures.get(x + 0));  //Sample index
                        mRawWriter.writeInt(gestures.get(x + 1));	//speed
                        mRawWriter.writeInt(gestures.get(x + 2));  //angle
                    }

                    //Write ending
                    mRawWriter.close();

                    //Rewrite number of audio samples
                    RandomAccessFile fout = new RandomAccessFile(rawFile, "rw");
                    FileChannel fc = fout.getChannel();
                    fc.position(13);
                    fc.write(ByteBuffer.wrap(new byte[] {
                            (byte)(numSamples >> 24),		//Number of samples
                            (byte)(numSamples >> 16),
                            (byte)(numSamples >> 8),
                            (byte)(numSamples >> 0),
                            (byte)(gestures.size() / 3),	//Number of direction samples
                            (byte)(lengthOfRecord >> 56),   //Length of sample in nanoseconds
                            (byte)(lengthOfRecord >> 48),
                            (byte)(lengthOfRecord >> 40),
                            (byte)(lengthOfRecord >> 32),
                            (byte)(lengthOfRecord >> 24),
                            (byte)(lengthOfRecord >> 16),
                            (byte)(lengthOfRecord >> 8),
                            (byte)(lengthOfRecord >> 0)
                    }));
                    fc.close();

                } catch (Exception e) { }

                //Send done signal
                sendMessage(gesture, -1);

                //Wait a second to start next test
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) { }
            }

            //Done!
            if(condition != Condition.INACTIVE)
                sendMessage(mGestures.get(mGestures.size() - 1), -1, true);
        }

        private void sendMessage(Gesture g, long time, boolean done) {
            Log.v(TAG, "Test update: " + g.getName() + " at " + time);
            mHandler.obtainMessage(0, new TestState(g, time, done)).sendToTarget();
        }

        private void sendMessage(Gesture g, long time) {
            Log.v(TAG, "Test update: " + g.getName() + " at " + time);
            mHandler.obtainMessage(0, new TestState(g, time)).sendToTarget();
        }
    }

    private class TestState {
        final Gesture currentGesture;
        final long countdownTime;
        boolean done = false;

        TestState(Gesture g, long time, boolean done) {
            currentGesture = g;
            countdownTime = time;
            this.done = done;
        }

        TestState(Gesture g, long time) {
            this(g, time, false);
        }
    }
}
