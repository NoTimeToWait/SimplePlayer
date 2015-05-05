package com.notime2wait.simpleplayer.visualization;

import android.graphics.Bitmap;

import com.notime2wait.simpleplayer.MusicData.Track;

public interface IVisuals {
	
	public interface OnVisualsUpdateListener {
			public void onVisualsUpdate(Bitmap cachedBitmap);
	}
	
	//public boolean isReady();
	
	public void setOnVisualsUpdateListener(OnVisualsUpdateListener visUpdateListener);
	
	//public float[] getWaveformCoords() ;
	
	//public void setTrack(Track track);

}
