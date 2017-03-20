package edu.smu.lyle.ultragesture;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

class FrequencyEmitter {

	private static final String TAG = "FrequencyEmitter";
	private AudioTrack mAudioTrack;

    static final int FREQUENCY = 16000;

	FrequencyEmitter() {
		//Get the size of the tone buffer
		//Should be a multiple of the frequency
        int size = (int)((double) AudioPoller.SAMPLE_RATE * FREQUENCY / AudioPoller.SAMPLE_RATE);
		size = (int)(size * AudioPoller.SAMPLE_RATE / FREQUENCY);
		
		//Create tone variables
		double[] inPhaseTone = new double[AudioPoller.SAMPLE_RATE / (int) FREQUENCY];
		double[] quadratureTone = new double[AudioPoller.SAMPLE_RATE / (int) FREQUENCY];
		
		//Create the tone being used
		byte tone[] = new byte[size * 2];
		for(int x = 0; x < tone.length / 2; x+=2) {
			//Get values for in phase and quadrature
			double value = Math.sin(2 * Math.PI * x * FREQUENCY / AudioPoller.SAMPLE_RATE);
			double qvalue = Math.sin(2 * Math.PI * x * FREQUENCY / AudioPoller.SAMPLE_RATE + Math.PI / 2);
			
			//Update tones accordingly
			if(x / 2 < inPhaseTone.length) {
				inPhaseTone[x / 2] = value;
				
				//Quadrature is phase shifted 90deg (or pi/2)
				quadratureTone[x / 2] = qvalue;
				
				//Log.v(TAG, String.format("tone[%d], i=%.2f, q=%.2f", x / 2, inPhaseTone[x / 2], quadratureTone[x / 2]));
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
                AudioPoller.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, tone.length,
                AudioTrack.MODE_STATIC);
		
		//Set the tone
		mAudioTrack.write(tone, 0, tone.length);
	
		//Set to loop forever
		mAudioTrack.reloadStaticData();
		int success = mAudioTrack.setLoopPoints(0, (int)(AudioPoller.SAMPLE_RATE / FREQUENCY) * 100, -1);
		if(success != AudioTrack.SUCCESS) {
			Log.e(TAG, "Error setting loop points: " + success);
		}
	}
	

	void start() {
		mAudioTrack.play();
	}
	
	void stop() {
		mAudioTrack.stop();
	}
}
