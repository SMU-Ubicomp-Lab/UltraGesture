package edu.smu.lyle.ultragesture;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import edu.samsung.ultragesture.R;
import edu.smu.lyle.ultragesture.AudioPoller.PolledDataCallback;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class UltraGetsureRaw extends Activity implements PolledDataCallback {

	private static final String TAG = "UltraGesture";
	
	//Data classes
	private AudioPoller mAudioPoller;
	private FrequencyEmitter mFreqEmitter;
	
	//Arrays for holding expected tones
	private double[] mITone;
	private double[] mQTone;
	private int mToneIndex;
	
	//Views
	private XYPlot mPlot;
	private Button mRecordButton;
	
	//Graph update
	private static final long UPDATE_DELAY = 100;
	private long mLastUpdate = 0l;
	
	//Recording variables
	private boolean mRecording = false;
	private DataOutputStream mRawWriter, mIWriter, mQWriter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ultra_getsure_raw);
		
		//Get the plot
		mPlot = (XYPlot)findViewById(R.id.xy_plot);
		//mPlot.setRangeBoundaries(Short.MIN_VALUE, Short.MAX_VALUE, BoundaryMode.FIXED);
		
		//Get the record button and set on click listener
		mRecordButton = (Button)findViewById(R.id.record_button);
		mRecordButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleRecording();
			}
		});
		
		//Create audio poller, set this as a callback
		mAudioPoller = new AudioPoller();
		mAudioPoller.setPolledDataCallback(this);
		
		//Create a frequency emitter
		mFreqEmitter = new FrequencyEmitter(16000f, 48000);
		
		//Set tones
		mITone = mFreqEmitter.getInPhaseTone();
		mQTone = mFreqEmitter.getQuadTone();
		
		//Keep the screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//Start polling audio data
		mAudioPoller.start(512);
		
		//Start playing sound
		mFreqEmitter.start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		//Stop recording
		if(mRecording)
			toggleRecording();
		
		//Stop polling on pause
		if(mAudioPoller != null)
			mAudioPoller.stop();
		
		//Stop sound
		if(mFreqEmitter != null)
			mFreqEmitter.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ultra_getsure, menu);
		return true;
	}

	@Override
	public void onPolledData(short[] pcmData) {
		//Update after a delay
		if(mLastUpdate + UPDATE_DELAY < System.currentTimeMillis()) {
			updatePlot(pcmData);
			mLastUpdate = System.currentTimeMillis();
		}
		
		//Increment tone index
		mToneIndex = (mToneIndex + pcmData.length) % mITone.length;
		
		//Write to file if recording
		if(mRecording) {
			try {
				//Write each short
				for(int x = 0; x < pcmData.length; x++) {
					//Write raw data
					mRawWriter.writeShort(pcmData[x]);
					
					//Calc tone index
					int toneIndex = (mToneIndex + x) % mITone.length;
					
					//Write I and Q tone to files
					mIWriter.writeShort((short)(Short.MAX_VALUE * mITone[toneIndex]));
					mQWriter.writeShort((short)(Short.MAX_VALUE * mQTone[toneIndex]));
				} 
			} catch (IOException e) {
				Log.e(TAG, "Error writing data to output stream.", e);
			}
		}
	}
	
	private void updatePlot(short[] pcmData) {
		//Re-add plot data (probably not the best way, but eh)
		SimpleXYSeries series = new SimpleXYSeries("Short Data");
		SimpleXYSeries series2 = new SimpleXYSeries("In phase Data");
		SimpleXYSeries series3 = new SimpleXYSeries("Quad Data");
		for(int i = 0; i < 18/*pcmData.length*/; i++) {
			series.addLast(i, pcmData[i]);
			series2.addLast(i, (short)(300 * mITone[(i + mToneIndex) % mITone.length]));
			series3.addLast(i, (short)(300 * mQTone[(i + mToneIndex) % mITone.length]));
		}
		
		mPlot.clear();
		mPlot.addSeries(series, new LineAndPointFormatter(0xFFFF0000, 0xFFFF0000, 0, null));
		mPlot.addSeries(series2, new LineAndPointFormatter(0xFF00FF00, 0xFF00FF00, 0, null));
		mPlot.addSeries(series3, new LineAndPointFormatter(0xFF0000FF, 0xFF0000FF, 0, null));
		mPlot.redraw();
	}
	
	private void toggleRecording() {
		if(mRecording) {
			Log.v(TAG, "Turing recording off");
			
			//Turn off recording
			mRecording = false;
			
			//Change button text
			mRecordButton.setText(R.string.start_record);
			
			//Complete file writing
			try {
				mRawWriter.flush();
				mRawWriter.close();
				
				mIWriter.flush();
				mIWriter.close();
				
				mQWriter.flush();
				mQWriter.close();
			} catch (IOException e) {
				Log.e(TAG, "Couldn't close file.", e);
			}
			
			//Set null
			mRawWriter = mIWriter = mQWriter = null;
		}
		else {
			Log.v(TAG, "Turing recording on");
			
			try {
				//Ensure directory exists
				File directory = new File("/storage/emulated/0/ultragesture/outputs/");
				if(!directory.exists()) {
					directory.mkdirs();
				}
				
				//Get date format for file writing
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.US);
				
				//Generate filename and files
				String filename = sdf.format(new Date());
				File rawFile = new File("/storage/emulated/0/ultragesture/outputs/" + filename + "_raw.dat");
				File iFile = new File("/storage/emulated/0/ultragesture/outputs/" + filename + "_i.dat");
				File qFile = new File("/storage/emulated/0/ultragesture/outputs/" + filename + "_q.dat");
				
				//Create writer objects
				mRawWriter = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rawFile)));
				mIWriter = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(iFile)));
				mQWriter = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(qFile)));
			} catch(Exception e) {
				Log.e(TAG, "Couldn't create output files.", e);
				return;
			}
			
			//Change button text
			mRecordButton.setText(R.string.stop_record);
			
			//Turn on recording
			mRecording = true;
		}
	}
}
