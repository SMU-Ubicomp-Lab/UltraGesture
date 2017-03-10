package edu.smu.lyle.ultragesture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class AudioPoller {

	private static final String TAG = "AudioPoller";
	
	private AudioRecord mAudioRecord;
	private int mBufferSize;
	
	private Thread mAPThread;
	private boolean mStop;
	
	private PolledDataCallback mPolledCallback;
	private Handler mHandler;
	
	public static final int SAMPLE_RATE = 48000;
	
	public AudioPoller() {
		//Get the minimum buffer size
		mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		Log.v(TAG, "Buffer size: " + mBufferSize);
		
		//Create handler for callback
		mHandler = new Handler(Looper.getMainLooper()) {
			@Override
            public void handleMessage(Message inputMessage) {
				//Get data
				short[] pcmData = (short[])inputMessage.obj;
				
				//Send to callback
				if(mPolledCallback != null) {
					mPolledCallback.onPolledData(pcmData);
				}
            }
		};
	}
	
	public void start(int sampleSize) {
		//Check running thread
		if(mAPThread != null)
			return;
		
		//Don't stop yet
		mStop = false;
		
		//Create AudioRecord object
		mAudioRecord = new AudioRecord(AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize);
		mAudioRecord.startRecording();
		
		//Run loop
		mAPThread = new Thread(new APRunnable(sampleSize));
		mAPThread.start();
	}
	
	public void stop() {
		//Check running thread
		if(mAPThread != null) {
			
			//Send stop
			mStop = true;
			
			//Wait for thread to finish to continue
			try {
				mAPThread.join();
			} catch (InterruptedException e) { }
			
			//Stop recording
			if(mAudioRecord != null) {
				mAudioRecord.stop();
				mAudioRecord.release();
				mAudioRecord = null;
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		
		if(mAudioRecord != null) {
			//Stop and release audio record object
			mAudioRecord.stop();
			mAudioRecord.release();
			mAudioRecord = null;
		}
	}
	
	public void setPolledDataCallback(PolledDataCallback pdc) {
		mPolledCallback = pdc;
	}
	
	private class APRunnable implements Runnable {

		private int mSampleSize;
		
		public APRunnable(int sampleSize) {
			mSampleSize = sampleSize;
		}
		
		@Override
		public void run() {
			//State return val
			int state;
			
			//Run until stop
			while(!mStop) {
				//Create data (this is probably expensive!)
				short[] pcmData = new short[mSampleSize];
				
				//Read in data
				state = mAudioRecord.read(pcmData, 0, mSampleSize);
				
				if(state == AudioRecord.ERROR_INVALID_OPERATION)
					Log.e(TAG, "Invalid operation");
				else if(state == AudioRecord.ERROR_BAD_VALUE)
					Log.e(TAG, "Invalid arguments");
				else if(state < mSampleSize)
					Log.e(TAG, "Buffer not filled");
				
				//Send data to the target
				mHandler.obtainMessage(0, pcmData).sendToTarget();
			}
		}
		
	}
	
	public interface PolledDataCallback {
		void onPolledData(short[] pcmData);
	}
}
