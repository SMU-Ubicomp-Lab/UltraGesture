package edu.smu.lyle.ultragesture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.OperationCanceledException;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Arya on 6/13/17.
 */
class TestThread extends Thread {
    private final String TAG = "TrialThread";

    private final long COUNTDOWN_MILLISECONDS = 3000L;
    private FrequencyEmitter emitter;
    private final List<Gesture> mGestures;

    int trialID;
    Movement mLastMovement = new Movement();

    TestThread(int trialID) {
        this.trialID = trialID;
        //Get list of gestures
        mGestures = Gesture.getGestures(UltraGesture.getUltraGesture());
        Collections.shuffle(mGestures);

        emitter = new FrequencyEmitter();
    }

    private void sendMessage(Gesture g, long time, MessageType type) {
        Log.v(TAG, "Test update: " + g.getName() + " at " + time);
        UltraGesture.getUltraGesture().getThreadHandler().obtainMessage(0, new CountdownStatus(g, time, type)).sendToTarget();
    }

    private void sendTrialUpdate(int trialID) {

    }

    private void discoverGestures() {
        //Loop for each gesture
        Log.d(TAG, "Trial ID " + trialID);
        for (final Gesture gesture : mGestures) {
            //Generate filename, file, and writer
            File rawFile = UltraGesture.getUltraGesture().mStorage.getFile(UltraGesture.getUltraGesture().mUserText.getText().toString(), Integer.toString(trialID), gesture);

            ArrayList<Movement> movements = new ArrayList<>();
            int numSamples = 0;
            long lengthOfRecord = 0;

            while (true /* `break` when did detect movement */) {
                // Send initial message.
                sendMessage(gesture, COUNTDOWN_MILLISECONDS, MessageType.SUCCESS);
                displayInstructions(gesture);

                // Start emitting frequency.
                emitter.start();
                Log.d(TAG, "Emitter started!");

                numSamples = 0;

                // Reset gesture data.
                movements = new ArrayList<>();
                mLastMovement = Movement.clear();

                lengthOfRecord = 0;

                // Start writing to file.
                try (final DataOutputStream rawWriter = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rawFile)))) {

                    // Write header
                    rawWriter.writeByte(0); //Revision
                    rawWriter.writeInt(AudioPoller.SAMPLE_RATE); //Sample rate
                    rawWriter.writeInt(FrequencyEmitter.FREQUENCY); //Frequency 1
                    rawWriter.writeInt(0); //Frequency 2
                    rawWriter.writeInt(0); //Number of audio samples (will rewrite later)
                    rawWriter.writeByte(0); //Number of direction samples
                    rawWriter.writeLong(0); //Length of audio sample (in nanoseconds)

                    //Create the audio recorder
                    int bufferSize = AudioRecord.getMinBufferSize(AudioPoller.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    AudioRecord mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioPoller.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                    mAudioRecord.startRecording();

                    //Create and start the audio poller
                    short pcmData[] = new short[512];
                    int state;
                    long startTime = System.nanoTime();
                    while (startTime + 3000000000L > System.nanoTime()) {
                        //Read in data
                        state = mAudioRecord.read(pcmData, 0, 512);
                        if (state == AudioRecord.ERROR_INVALID_OPERATION)
                            Log.e(TAG, "Invalid operation");
                        else if (state == AudioRecord.ERROR_BAD_VALUE)
                            Log.e(TAG, "Invalid arguments");
                        else if (state < 512)
                            Log.e(TAG, "Buffer not filled");

                        //Write data to file
                        for (int x = 0; x < state; x++)
                            rawWriter.writeShort(pcmData[x]);

                        //Increment count
                        numSamples += state;

                        if (mLastMovement.isValid()) {
                            Log.v(TAG, "Gesture detected");

                            mLastMovement.index = numSamples;

                            //Add data to list with timestamp
                            movements.add(mLastMovement);

                            //Reset gesture
                            mLastMovement = Movement.clear();
                        }
                    }


                    //Get the length of the recording
                    lengthOfRecord = System.nanoTime() - startTime;

                    //Stop recording
                    mAudioRecord.stop();
                    mAudioRecord.release();

                    //Stop emitting signal
                    emitter.stop();
                    Log.d(TAG, "Emitter stopped!");

                    boolean didDetectMovement = (movements.size() > 0);

                    if (!didDetectMovement) {
                        sendMessage(gesture, -1, MessageType.FAILURE);
                        try {
                            Thread.sleep(2000L);
                        } catch (InterruptedException e) {
                            /* No need to die because of this. */
                        }
                    } else {
                        for (int x = 0; x < movements.size(); x++) {
                            rawWriter.writeInt(movements.get(x).index);  //Sample index
                            rawWriter.writeInt(movements.get(x).speed);    //speed
                            rawWriter.writeInt(movements.get(x).angle);  //angle
                        }
                        break;
                    }

                } catch (IOException e) {
                    Log.e(TAG, "IOException in writing to file.", e);
                    System.exit(-1);
                }
            }

            updateHeader(rawFile, numSamples, movements, lengthOfRecord);

            //Send doneWithAllGestures signal
            sendMessage(gesture, -1, MessageType.SUCCESS);

            //Wait a second to start next test
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                /* No need to die because of this. */
            }
        }

        //Done!
        if (UltraGesture.getUltraGesture().condition != Condition.INACTIVE)
            sendMessage(mGestures.get(mGestures.size() - 1), -1, MessageType.ALL_DONE);
    }

    @Override
    public void run() {
        try {
            discoverGestures();
        } catch (OperationCanceledException e) {
            Log.i(TAG, "Trial canceled.", e);
            // pass
        }
    }

    private void updateHeader(File rawFile, int numSamples, ArrayList<Movement> gestures, long lengthOfRecord) {
        try (FileChannel headerUpdater = new RandomAccessFile(rawFile, "rw").getChannel()) {
            //Rewrite number of audio samples
            headerUpdater.position(13);
            headerUpdater.write(ByteBuffer.wrap(new byte[]{
                    (byte) (numSamples >> 24),        //Number of samples
                    (byte) (numSamples >> 16),
                    (byte) (numSamples >> 8),
                    (byte) (numSamples >> 0),
                    (byte) (gestures.size()),    //Number of direction samples
                    (byte) (lengthOfRecord >> 56),   //Length of sample in nanoseconds
                    (byte) (lengthOfRecord >> 48),
                    (byte) (lengthOfRecord >> 40),
                    (byte) (lengthOfRecord >> 32),
                    (byte) (lengthOfRecord >> 24),
                    (byte) (lengthOfRecord >> 16),
                    (byte) (lengthOfRecord >> 8),
                    (byte) (lengthOfRecord >> 0)
            }));
            headerUpdater.close();

        } catch (IOException e) {
            Log.e(TAG, "Couldn't update file header.");
            System.exit(-2);
        }
    }

    private void displayInstructions(Gesture gesture) {
        //Start countdown
        long goalTime = System.currentTimeMillis() + COUNTDOWN_MILLISECONDS;
        long nextGoal = COUNTDOWN_MILLISECONDS - 1000L;

        long remainingTime;
        boolean lastPaused = false;
        while (nextGoal >= 0) {
            //Cancel logic
            if (UltraGesture.getUltraGesture().condition == Condition.INACTIVE) {
                throw new OperationCanceledException();
            }

            //Pausing logic
            if (UltraGesture.getUltraGesture().condition == Condition.PAUSED) {
                goalTime = System.currentTimeMillis() + COUNTDOWN_MILLISECONDS;
                nextGoal = COUNTDOWN_MILLISECONDS - 1000L;

                if (!lastPaused)
                    sendMessage(gesture, COUNTDOWN_MILLISECONDS, MessageType.SUCCESS);

                lastPaused = true;
                continue;
            } else {
                lastPaused = false;
            }

            //Get the current time remaining
            remainingTime = goalTime - System.currentTimeMillis();
            if (remainingTime < nextGoal) {
                sendMessage(gesture, nextGoal, MessageType.SUCCESS);
                nextGoal -= 1000L;
            }
        }
    }
}
