package edu.smu.lyle.ultragesture;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class FrequencyEmitter {

	private static final String TAG = "FrequencyEmitter";
	private AudioTrack mAudioTrack;
	
	private double[] mITone;
	private double[] mQTone;
	
	public FrequencyEmitter(float frequency, int sampleRate) {
		//Get the size of the tone buffer
		//Should be a multiple of the frequency
		double min = sampleRate;
		int size = (int)(min * frequency / sampleRate);
		size = (int)(size * sampleRate / frequency);
		
		//Create tone variables
		mITone = new double[sampleRate / (int)frequency];
		mQTone = new double[sampleRate / (int)frequency];
		
		//Create the tone being used
		byte tone[] = new byte[size * 2];
		for(int x = 0; x < tone.length / 2; x+=2) {
			//Get values for in phase an quadrature
			double value = Math.sin(2 * Math.PI * x * frequency / sampleRate);
			double qvalue = Math.sin(2 * Math.PI * x * frequency / sampleRate + Math.PI / 2);
			
			//Update tones accordingly
			if(x / 2 < mITone.length) {
				mITone[x / 2] = value;
				
				//Quadrature is phase shifted 90deg (or pi/2)
				mQTone[x / 2] = qvalue;
				
				//Log.v(TAG, String.format("tone[%d], i=%.2f, q=%.2f", x / 2, mITone[x / 2], mQTone[x / 2]));
			}
			
			//Convert to short for output
			short svalue = (short)(Short.MAX_VALUE * value);
			short sqvalue = (short)(Short.MAX_VALUE * qvalue);
			
			//Put in little endian
			tone[2*x] = (byte)(svalue & 0x00ff);
			tone[2*x+1] = (byte)((svalue & 0xff00) >>> 8);
			tone[2*x+2] = (byte)(sqvalue & 0x00ff);
			tone[2*x+3] = (byte)((sqvalue & 0xff00) >>> 8);
		}
		
		//Create audio track
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, tone.length,
                AudioTrack.MODE_STATIC);
		
		//Set the tone
		mAudioTrack.write(tone, 0, tone.length);
	
		//Set to loop forever
		mAudioTrack.reloadStaticData();
		int success = mAudioTrack.setLoopPoints(0, (int)(sampleRate / frequency) * 100, -1);
		if(success != AudioTrack.SUCCESS) {
			Log.e(TAG, "Error setting loop points: " + success);
		}
	}
	
	public double[] getInPhaseTone() {
		return mITone.clone();
	}
	
	public double[] getQuadTone() {
		return mQTone.clone();
	}
	
	public void start() {
		mAudioTrack.play();
	}
	
	public void stop() {
		mAudioTrack.stop();
	}
}
