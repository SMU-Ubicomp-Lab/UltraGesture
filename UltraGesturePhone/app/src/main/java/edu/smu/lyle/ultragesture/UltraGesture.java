package edu.smu.lyle.ultragesture;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.samsung.android.sdk.gesture.Sgesture;
import com.samsung.android.sdk.gesture.SgestureHand;
import com.samsung.android.sdk.gesture.SgestureHand.ChangeListener;
import com.samsung.android.sdk.gesture.SgestureHand.Info;

import edu.samsung.ultragesture.R;
import edu.smu.lyle.ultragesture.AudioPoller.PolledDataCallback;
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
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class UltraGesture extends Activity implements OnClickListener, ChangeListener {

	private static final String TAG = "UltraGesture";
	private static final String TEST_ID = "TEST_ID";
	
	private Button mStartButton;
	private Button mRestartButton;
	
	private TextView mGestureText;
	private TextView mDescText;
	private TextView mCountdownText;
	
	private TestThread mTestThread;
	
	private boolean paused = false;
	private boolean started = false;

	private Sgesture mSGesture = null;
	private SgestureHand mSGestureHand = null;
	private int mLastSpeed = -1;
	private int mLastAngle = -1;
	
	private Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
        public void handleMessage(Message inputMessage) {
			//Get the current state
            TestState state = (TestState)inputMessage.obj;
            
            //Display info
            mGestureText.setText(state.currentGesture.getName());
            mDescText.setText(state.currentGesture.getDesc());
            
            if(state.countdownTime > 0) {
            	mCountdownText.setText((int)Math.ceil(state.countdownTime / 1000.0) + "");
            }
            else if(state.countdownTime == 0) {
            	mCountdownText.setText("GO!");
            }
            else if(state.countdownTime == -1) {
            	mCountdownText.setText("Done!");
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

		//Grab widgets
		mStartButton = (Button)findViewById(R.id.start_stop_button);
		mStartButton.setOnClickListener(this);
		
		mRestartButton = (Button)findViewById(R.id.restart_button);
		mRestartButton.setOnClickListener(this);
		
		mGestureText = (TextView)findViewById(R.id.gesture_title);
		mDescText = (TextView)findViewById(R.id.gesture_description);
		mCountdownText = (TextView)findViewById(R.id.countdown);
		
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
		
		//Create sgesture
		mSGesture = new Sgesture();
		
		try {
			mSGesture.initialize(this);
		} catch(Exception e) {
			Log.e(TAG, "Couldn't load sgesture", e);
		}
		
		//Create sgesture hand
		mSGestureHand = new SgestureHand(Looper.getMainLooper(), mSGesture);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		//Stop the test on pause
		pauseTest();
	}
	
	/*
	 * Test controls
	 */
	
	private void startTest() {
		//Adjust UI
		mStartButton.setText("pause");
		mStartButton.setBackgroundColor(getResources().getColor(R.color.g_yellow));
		mRestartButton.setEnabled(false);
		
		//Update state
		started = true;
		paused = false;
		
		//Start gesture listener
		mSGestureHand.start(0, this);
		
		//Create thread
		mTestThread = new TestThread();
		mTestThread.start();
	}
	
	private void resumeTest() {
		//Adjust UI
		mStartButton.setText("pause");
		mStartButton.setBackgroundColor(getResources().getColor(R.color.g_yellow));
		mRestartButton.setEnabled(false);
		
		//Update state
		started = true;
		paused = false;
	}
	
	private void pauseTest() {
		//Adjust UI
		mStartButton.setText("resume");
		mStartButton.setBackgroundColor(getResources().getColor(R.color.g_blue));
		mRestartButton.setEnabled(true);
		
		//Update state
		started = true;
		paused = true;
	}
	
	private void stopTest(boolean save) {
		//Adjust UI
		mStartButton.setText("start");
		mStartButton.setBackgroundColor(getResources().getColor(R.color.g_blue));
		mRestartButton.setEnabled(false);
		
		//Update state
		started = false;
		paused = true;
		
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

            mRestartButton.setEnabled(false);
			mGestureText.setText("Finished!");
			mDescText.setText("Thank you for participating.");
		}
	}
	
	
	@Override
	public void onClick(View v) {
		if(v.equals(mStartButton)) {
			if(mStartButton.getText().equals("start")) {
				//Run the test
				startTest();
			}
			else if(mStartButton.getText().equals("resume")) {
				//Resume the test
				resumeTest();
			}
			else if(mStartButton.getText().equals("pause")) {
				//Pause the test
				pauseTest();
			}
		}
		else if(v.equals(mRestartButton)) {
			//Stop the test, don't save data
			stopTest(false);
		}
	}
	
	@Override
	public void onChanged(Info info) {
		//Set the gesture data here 
		mLastSpeed = info.getSpeed();
		mLastAngle = info.getAngle();
	}
	
	private class TestThread extends Thread {
		
		private List<Gesture> mGestures;
		
		private final long TIME_DELAY = 3000l;
		
		FrequencyEmitter emitter;
		
		public TestThread() {
			//Get list of gestures
			mGestures = Gesture.getGestures(UltraGesture.this);
			Collections.shuffle(mGestures);
			
			emitter = new FrequencyEmitter(16000f, 48000);
		}
		
		@Override
		public void run() {		
			//Loop for each gesture
			gloop:for(Gesture gesture : mGestures) {
				//Send initial message
				sendMessage(gesture, TIME_DELAY);
				
				//Start countdown
				long goalTime = System.currentTimeMillis() + TIME_DELAY;
				long nextGoal = TIME_DELAY - 1000l;
				
				long curTime;
				boolean lastPaused = false;
				while(nextGoal >= 0) {
					//Cancel logic
					if(!started) {
						break gloop;
					}
					
					//Pausing logic
					if(paused) {
						goalTime = System.currentTimeMillis() + TIME_DELAY;
						nextGoal = TIME_DELAY - 1000l;
						
						if(!lastPaused)
							sendMessage(gesture, TIME_DELAY);
						
						lastPaused = true;
						continue;
					}
					lastPaused = false;
					
					//Get the current time remaining
					curTime = goalTime - System.currentTimeMillis();	
					if(curTime < nextGoal) {
						sendMessage(gesture, nextGoal);
						nextGoal -= 1000l;
					}
				}	
				
				//Start emitting frequency
				emitter = new FrequencyEmitter(16000f, 48000);
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
					mRawWriter.writeInt(48000); //Sample rate
					mRawWriter.writeInt(16000); //Frequency 1
					mRawWriter.writeInt(0); //Frequency 2
					mRawWriter.writeInt(0); //Number of audio samples (will rewrite later)
					mRawWriter.writeByte(0); //Number of direction samples
					mRawWriter.writeLong(0); //Length of audio sample (in nanoseconds)
					
					//Create the audio recorder
					int mBufferSize = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
					AudioRecord mAudioRecord = new AudioRecord(AudioSource.MIC, 48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize);
					mAudioRecord.startRecording();
					
					//Reset gesture data
					ArrayList<Integer> gestures = new ArrayList<Integer>();
					mLastAngle = mLastSpeed = -1;
					
					//Create and start the audio poller
					short pcmData[] = new short[512];
					int state;
					int numSamples = 0;
					long startTime = System.nanoTime();
					while(startTime + 1000000000l > System.nanoTime()) {
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
			if(started)
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
		public Gesture currentGesture;
		public long countdownTime;
		public boolean done = false;
		
		public TestState(Gesture g, long time, boolean done) {
			currentGesture = g;
			countdownTime = time;
			this.done = done;
		}
		
		public TestState(Gesture g, long time) {
			this(g, time, false);
		}
	}
}
