package com.notime2wait.simpleplayer;


import android.app.Activity;
import android.widget.ArrayAdapter;

import java.util.ArrayList;


/*
 * The only implementation of this interface must be in MusicData (or MusicService) class and nowhere else
 * because all playlist changes should go through a single class which also must provide an Undo option via UndoController 
 */

public interface IPlaylist<T>  {
		
	public String getTitle();
	
	public void setTitle(String playlistName);
	
	public boolean hasDefaultTitle();
	
	//IMPORTANT:This getters should be used only in playback calls
	public T getTrack(int index);
	
	public int getPlaylistSize();
	
	public T getNext();
	
	public T getPrev();
	
	public T getFirst();
	
	public T getLast();

	public T remove(int position, boolean generateUndoEvent);
	
	public T remove(int position);
	
	public void move(int from, int to);
	
	public boolean add( int position, T item, boolean generateUndoEvent);
	
	public boolean add(T track, boolean generateUndoEvent);
	
	//public boolean add(T[] tracks);
	
	public int setCurrentTrackIndex(int i);
	
	public T getCurrentTrack();
	
	public int getCurrentTrackIndex();
	
	public T[] getTracksArray();
	
	public ArrayList<T> getTracks();
	
	//IMPORTANT:fragments should interact with playlist only via ArrayAdapters. All direct interaction with playlist should be prohibited
	public ArrayAdapter<T> getPlayListAdapter(Activity activity, int resource);

}

