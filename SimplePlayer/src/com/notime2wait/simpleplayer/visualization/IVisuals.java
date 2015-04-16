package com.notime2wait.simpleplayer.visualization;

import com.notime2wait.simpleplayer.MusicData.Track;

public interface IVisuals {
	
	public boolean isReady();
	
	public float[] getWaveformCoords() ;
	
	public void setTrack(Track track);

}
