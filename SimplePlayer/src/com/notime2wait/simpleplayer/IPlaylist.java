package com.notime2wait.simpleplayer;


import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;

import com.notime2wait.simpleplayer.MusicData.Track;

/*
 * The only implementation of this interface must be in MusicData (or MusicService) class and nowhere else
 * because all playlist changes should go through a single class which also must provide an Undo option via UndoController 
 */

public interface IPlaylist<T>  {
		
	public String getName();
	
	public void setName(String playlistName);
	
	//IMPORTANT:This getters should be used only in playback calls
	public T getTrack(int index);
	
	public T getNext();
	
	public T getPrev();
	
	public T getFirst();
	
	public T getLast();
	
	public T remove(int position);
	
	public void move(int from, int to);
	
	public boolean add( int position, T item);
	
	public boolean add(T track);
	
	public boolean add(T[] tracks);
	
	public void setCurrentTrackIndex(int i);
	
	public T getCurrentTrack();
	
	public int getCurrentTrackIndex();
	
	public T[] getTracksArray();
	
	//IMPORTANT:this should be used only in ArrayAdapters. All direct interaction with playlist should be prohibited
	public ArrayAdapter<T> getPlayListAdapter(Activity activity, int resource);

}

