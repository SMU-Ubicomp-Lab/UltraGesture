package edu.smu.lyle.ultragesture;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import edu.samsung.ultragesture.R;

class Gesture {
	
	private String mName;
	private String mDesc;
	private String mSName;
	
	//No instantiations
	private Gesture(String n, String s, String d)
	{
		mName = n;
		mSName = s;
		mDesc = d;
	}
	
	String getName() {
		return mName;
	}
	
	String getShortName() {
		return mSName;
	}
	
	String getDesc() {
		return mDesc;
	}
	
	static List<Gesture> getGestures(Context c) {
		//Create list
		List<Gesture> out = new ArrayList<>();
		
		//Populate from string resources
		String names[] = c.getResources().getStringArray(R.array.gestures);
		String shorts[] = c.getResources().getStringArray(R.array.gesture_shorts);
		String descs[] = c.getResources().getStringArray(R.array.gesture_descs);
		
		for(int x = 0; x < names.length; x++) {
			out.add(new Gesture(names[x], shorts[x], descs[x]));
		}
		
		return out;
	}
}
