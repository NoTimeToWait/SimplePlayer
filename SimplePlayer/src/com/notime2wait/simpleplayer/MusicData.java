package com.notime2wait.simpleplayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeMap;

import com.notime2wait.simpleplayer.UndoBarController.Undoable;
import com.notime2wait.simpleplayer.visualization.WaveformUtils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MusicData {
	
	private static String LOG_TAG = MusicData.class.getName();
	
	private MainActivity mMainActivity;
	
	//private Uri musicSourceUri; 

	/*
	 * array to contain music folder names 
	 */
	//private String[] mFolders; 
	
	/*
	 * array to contain playlists 
	 */
	//private ArrayList<IPlaylist> mPlaylists;
	
	/*
	 *  array with all music tracks  sorted by their titles
	 */
	private Track[] mTracks;
	/*
	 *  TreeMap with key set of folder names and values are arraylists of tracks that belong to respective folder
	 */
	private TreeMap<String, ArrayList<Track>> mFolderTracks;
	
	private Playlist mCurrentPlaylist = new Playlist();
	
	private PlaylistDbHelper mPlaylistDbHelper;
	
	private int HISTORY_LEN = 20;
	private LinkedList<IPlaylist<Track>> mPlaylistHistory = new LinkedList<IPlaylist<Track>>();
	private int mHistoryIndex = -1;
	//TODO: add current playlist int num
	//private int mCurrentTrackIndex;
	
	public void init(MainActivity activity) {
		mFolderTracks = new TreeMap<String, ArrayList<Track>>();
		mMainActivity = activity;
		getMusicList(false);
		mPlaylistDbHelper = new PlaylistDbHelper(mMainActivity);
	}
	
	public String getAlbumArt(Track track) {
		/*Cursor cursor = mMainActivity.managedQuery(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, 
                new String[] {MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_ART}, 
                MediaStore.Audio.Albums.ALBUM+ "=?", 
                new String[] {String.valueOf(track.getAlbum())}, 
                null);*/
		//TODO add the album art path to track info in main MediaDB access
		Cursor cursor;
		Uri musicSourceUri =  MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
		String[] proj = {MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_ART};
        String select = MediaStore.Audio.Albums.ALBUM+ "=?"; //"("+MediaStore.Audio.Media.IS_MUSIC + " != 0) AND (" + MediaStore.Audio.Media.DATA +" != '')";
        String[] args = {String.valueOf(track.getAlbum())};
		//String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
		
		CursorLoader loader = new CursorLoader(mMainActivity, musicSourceUri, proj, select, args, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
		cursor = loader.loadInBackground();
		
		
		String path = "";
		if (cursor.moveToFirst()) 
			path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
		return path;
	}
	
	public Playlist getCurrentPlaylist() {
		return mCurrentPlaylist;
	}
	
	public ListIterator<IPlaylist<Track>> getPlaylistHistory() {
		if (MainActivity.DEBUG) Log.e("Iterator", "HistoryListLen="+mPlaylistHistory.size()+"HistoryIndex="+mHistoryIndex);
		return  mPlaylistHistory.listIterator(mHistoryIndex<0? 0 : mHistoryIndex);
		/*TODO: Add here current playlist if num is out of bounds*/
	}
	
	public int getHistoryIndex() {
		return mHistoryIndex;
	}
	
	public void erasePlaylistHistory(){
		mHistoryIndex=-1;
		mPlaylistHistory = new LinkedList<IPlaylist<Track>>();
	}
	
	public int getHistorySize() {
		return mPlaylistHistory.size();
	}
		
	public void setHistoryIndex(int index) {
		mHistoryIndex = index;
	}
	
	public boolean isHomePlaylist(){
		if (mPlaylistHistory.isEmpty()) return true;
		if (MainActivity.DEBUG) Log.e("1111111", "mHistoryIndex="+mHistoryIndex+" CurrentPlaylist="+mCurrentPlaylist.title);
		return mCurrentPlaylist.equals(mPlaylistHistory.get(mHistoryIndex<0? 0:mHistoryIndex));
	}
	
	public Set<String> getFolders() {
		return mFolderTracks.keySet();
	}
	
	public Track[] getTracks() {
		return mTracks;
	}
	

	public Track[] getTracks(String folderName)	{
		if (mFolderTracks.containsKey(folderName))
			return mFolderTracks.get(folderName).toArray(new Track[0]);
		return new Track[0];
	}
	/*
	public String[] getPaths(String folderName)	{
		if (MainActivity.DEBUG)
			for (String str:mFolders) Log.d(LOG_TAG, "mFolders"+str);
		if (MainActivity.DEBUG)
			Log.d(LOG_TAG, "Folder Index="+Arrays.binarySearch(mFolders, folderName));
		if (MainActivity.DEBUG)
			Log.d(LOG_TAG, "Folde="+mFolders[Arrays.binarySearch(mFolders, folderName)]);
		
		return getTracks(Arrays.binarySearch(mFolders, folderName));
	}*/
	
	
	
	/*
	public Track[] getTracks(int folderNum)	{
		int offset = folderNum == 0    ?    0    :    mFolderTracks.get(folderNum);
		int size = folderNum == mFolders.length-1    ?    mTracks.length    :    mFolderTracks.get(folderNum+1);
		size-=offset;
		if (MainActivity.DEBUG)
			Log.e(LOG_TAG, "Size="+size+"Offset="+offset);
		Track[] folder_tracks = new Track[size];
		System.arraycopy(mTracks, offset, folder_tracks, 0, size);
		return folder_tracks;
	}*/
	
	/*
	public int getOffset(String folderName) {
		return mFolderTracks.get(Arrays.binarySearch(mFolders, folderName));
	}*/
	
	/*
	public int getTrackIndex() {
		return mCurrentTrackIndex;
	}*/
	
	public Track getCurrentTrack() {
		return mCurrentPlaylist.getCurrentTrack();
	}
	

	
	private void getMusicList(boolean internal_storage) {
		Cursor cursor;
		Uri musicSourceUri = !internal_storage&&this.isSdMounted()? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI :  MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        String select = "("+MediaStore.Audio.Media.IS_MUSIC + " != 0) AND (" + MediaStore.Audio.Media.DATA +" != '')";
		String[] proj = { MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST };
		//String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
		if (MainActivity.DEBUG)
			Log.d(LOG_TAG, "Selected music uri:"+musicSourceUri);
		
		CursorLoader loader = new CursorLoader(mMainActivity, musicSourceUri, proj, select, null, MediaStore.Audio.Media.TITLE);
		cursor = loader.loadInBackground();
		

		//ArrayList<String> folderlist = new ArrayList<String>();
		
		//TODO: make it async  
		if (cursor.moveToFirst()) {
				mTracks = new Track[cursor.getCount()];
				int i=0;
				String music_data, music_folder="", trackname, album, artist;
				do {
					
					music_data = cursor.getString(0);
					trackname = cursor.getString(1);
					album = cursor.getString(2);
					artist = cursor.getString(3);
					mTracks[i] = new Track(trackname, music_data, album, artist);
					if (MainActivity.DEBUG) Log.d(LOG_TAG, music_data);
					int slash_position = music_data.lastIndexOf('/');
					if (slash_position<2) continue;
					//trackname = music_data.substring(slash_position+1)+"+"+cursor.getString(1);
					music_data = music_data.substring(0, slash_position);
					if (!mFolderTracks.containsKey(music_data)) mFolderTracks.put(music_data, new ArrayList<Track>());
					
					mFolderTracks.get(music_data).add(mTracks[i]);
					
					/*
					if (mFolderTracks.containsKey(music_data)) {
						mFolderTracks.put(music_data, new Pair(i , i));
					}
					else {
						Pair<Integer, Integer> begin_end = mFolderTracks.get(music_data);
						if (i>begin_end.second) mFolderTracks.put(music_data, new Pair(begin_end.first , i));
					}
					*/
					/*
					if (!music_folder.equals(music_data)) {
						
						folderlist.add(music_data);
						mFolderTracks.add(i);
						music_folder = music_data;
					}*/
					
					i++;
				} while (cursor.moveToNext());
		}
		else {
			getMusicList(true);
			return;
		}
		cursor.close();
		//mFolders = new String[folderlist.size()];
		//mFolders = new String[mFolderTracks.keySet().size()];
		//mFolders = mFolderTracks.keySet().toArray(mFolders);//folderlist.toArray(mFolders);
	}
	
	public PlaylistDbHelper getPlaylistDbHelper() {
		/*try {
			return mPlaylistDbHelper.getReadableDatabase();
		}
		catch (SQLiteException e) {
			Log.e(LOG_TAG, "Unable to open playlist database");
			return null;
		}*/
		return  mPlaylistDbHelper;
	}
	
	public boolean isPlaying() {
		return mMainActivity.isPlaying();
	}
	
	public void stopMusic() {
		mMainActivity.stopMusic();
	}
	
	/**
	 * use only to add track and play it from AllMusicList
	 */
	
	public boolean playTrack(int trackNum) {
		mCurrentPlaylist = new Playlist();
		mCurrentPlaylist.add(mTracks[trackNum]);
		mCurrentPlaylist.setCurrentTrackIndex(0); //0 means that current track is the first track
		mPlaylistHistory.addLast(mCurrentPlaylist);
		mHistoryIndex = mPlaylistHistory.size()-1;
		return mMainActivity.playTrack(mTracks[trackNum], getAlbumArt(mTracks[trackNum]));
	}
	/*
	public boolean playTrack(Track track) {

		//mCurrentPlaylist.setCurrentTrackIndex(trackPosition);
		return mMainActivity.playTrack(track);
	}*/
	/**
	 * to call from tracklist frag
	 * @param trackPosition
	 * @param track
	 * @return
	 */
	public boolean playTrack(int trackPosition, IPlaylist playlist) {
		Track track = (Track) playlist.getTrack(trackPosition);
		mCurrentPlaylist = (Playlist) playlist;
		mCurrentPlaylist.setCurrentTrackIndex(trackPosition);
		return mMainActivity.playTrack(track, getAlbumArt(track));
	}
	
	/**
	 * to call from folderlist frag, allmusic list frag and album frag
	 * @param folder
	 * @param trackPosition
	 * @param totalTrackNum
	 */
	public boolean playTrack(String folder, int trackPosition, int totalTrackNum) {
		//mCurrentTrackIndex = trackPos;
		mCurrentPlaylist = new Playlist();
		//Track[] folder_tracks = new Track[totalTrackNum];
		//int track_offset = getOffset(folder);
		//System.arraycopy(mTracks, track_offset, folder_tracks, 0, totalTrackNum);
		Track[] folder_tracks = mFolderTracks.get(folder).toArray(new Track[0]);
		mCurrentPlaylist.add(folder_tracks);
		mCurrentPlaylist.setCurrentTrackIndex(trackPosition);
		mPlaylistHistory.addLast(mCurrentPlaylist);
		mHistoryIndex = mPlaylistHistory.size()-1;
		return mMainActivity.playTrack(folder_tracks[trackPosition], getAlbumArt(folder_tracks[trackPosition]));
	}
	
	public boolean playTracks(String playlist, int trackPosition, final Track[] tracks ) {
		//mCurrentTrackIndex = trackPos;
		mCurrentPlaylist = new Playlist();
		//TODO check if there tracks array is consistent
		mCurrentPlaylist.add(tracks);
		mCurrentPlaylist.setCurrentTrackIndex(trackPosition);
		mCurrentPlaylist.setTitle(playlist);
		mPlaylistHistory.addLast(mCurrentPlaylist);
		mHistoryIndex = mPlaylistHistory.size()-1;
		return mMainActivity.playTrack(tracks[trackPosition], getAlbumArt(tracks[trackPosition]));
	}
	/*
	public void addTrackToPlaylist(int globalTrackNum) {
		mCurrentPlaylist.add(mTracks[globalTrackNum]);
	}*/
	
	public void addTrackToPlaylist(String folder, int trackNum) {
		mCurrentPlaylist.add(mFolderTracks.get(folder).get(trackNum));
	}
	
	public void addTrackToPlaylist(Track track) {
		mCurrentPlaylist.add(track);
	}
	
	
	
	public void addTracksToPlaylist(String folderName, int totalTrackNum) {
		addTracksToPlaylist(folderName, totalTrackNum, mCurrentPlaylist);
	}
	
	public void addTracksToPlaylist(Track[] tracks) {
		if (!mCurrentPlaylist.add(tracks)) {
			mCurrentPlaylist = new Playlist();
			mCurrentPlaylist.add(tracks);
			mPlaylistHistory.addLast(mCurrentPlaylist);
		}
	}
	
	public void addTracksToPlaylist(String folderName, int totalTrackNum, Playlist playlist) {
		//Track[] folder_tracks = new Track[totalTrackNum];
		//int track_offset = getOffset(folderName);
		//System.arraycopy(mTracks, track_offset, folder_tracks, 0, totalTrackNum);
		Track[] folder_tracks = mFolderTracks.get(folderName).toArray(new Track[0]);
		if (!playlist.add(folder_tracks)) {
			mCurrentPlaylist = new Playlist();
			mCurrentPlaylist.add(folder_tracks);
			mPlaylistHistory.addLast(mCurrentPlaylist);
		}
	}
	/*
	public Uri getMusicSourceUri() {
		return musicSourceUri;
	}*/
	
	private boolean isSdMounted() 
	{
	    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
	
	
	private class Playlist implements IPlaylist<Track> {
		
		//contains track paths
		private String title;
		private final ArrayList<Track> playlist = new ArrayList<Track>();
		private ArrayAdapter<Track> mAdapter;
		private int mCurrentTrackIndex = 0;
		private UndoBarController mUndoPopupController;
		private int mLastRemovedIndex;
		
		public Playlist() {
			title = "UntitledPlaylist";
		}
		
		public Playlist(String playlistName) {
			title = playlistName;
		}
				
		public String getTitle() {
			return title;
		}
		
		public void setTitle(String playlistName) {
			title = playlistName;
		}
		
		public Track remove(int position) {
			mLastRemovedIndex = position;
			if (MainActivity.DEBUG) Log.e("Track remove0", " pos1 ="+position+"mCurrentTrackIndex:"+mCurrentTrackIndex);
			MainActivity.handleUndoAction(new Undoable(){

				private int pos = mLastRemovedIndex;
				private Track item = getTrack(pos).clone();
				@Override
				public void undo() {
					add(pos, item);
					//if (mAdapter!=null) mAdapter.notifyDataSetChanged();
				}

				@Override
				public String getUndoMessage() {
					return "Undo "+item.getTitle()+" remove";
				}
				
			});
			
			if (MainActivity.DEBUG) Log.e("Track remove1", " pos1 ="+position+"mCurrentTrackIndex:"+mCurrentTrackIndex);
			if (((position==mCurrentTrackIndex && position==playlist.size()-1)||
				(position < mCurrentTrackIndex))&&isHomePlaylist()) {
					mCurrentTrackIndex--;
			}
			if (MainActivity.DEBUG) Log.e("Track remove2", " pos1 ="+position+"mCurrentTrackIndex:"+mCurrentTrackIndex);
			Track item = playlist.remove(position);
			if (mAdapter!=null) mAdapter.notifyDataSetChanged();
			return item;
		}
		
		public boolean add( int position, Track item) {
			if (MainActivity.DEBUG) Log.e("Track remove:undo add0", " mCurrentTrackIndex ="+mCurrentTrackIndex);
			if (isHomePlaylist()) {
				if (position<=mCurrentTrackIndex) mCurrentTrackIndex++;
				if (mCurrentTrackIndex<0) mCurrentTrackIndex=0;
			}
			playlist.add(position, item);
			if (mAdapter!=null) mAdapter.notifyDataSetChanged();
			if (MainActivity.DEBUG) Log.e("Track remove:undo add1", " mCurrentTrackIndex ="+mCurrentTrackIndex);
			return true;
		}
		
		public boolean add(Track track) {
			if (isHomePlaylist()&&mCurrentTrackIndex<0) mCurrentTrackIndex=0;
			playlist.add(track);
			if (mAdapter!=null) mAdapter.notifyDataSetChanged();
			return true;
		}
		
		public boolean add(Track[] tracks) {
			if (mCurrentTrackIndex<0) mCurrentTrackIndex=0;
			Collections.addAll(playlist, tracks);
			if (mAdapter!=null) mAdapter.notifyDataSetChanged();
			return true;
		}
		
		@Override
		public void move(int from, int to) {
			if (from==to) return;
			if (isHomePlaylist()) {
				if (from<mCurrentTrackIndex && to>=mCurrentTrackIndex) mCurrentTrackIndex--;
				else if (from == mCurrentTrackIndex) mCurrentTrackIndex = to;
				else if (from>mCurrentTrackIndex && to<=mCurrentTrackIndex) mCurrentTrackIndex++;
			}
			Track item = playlist.remove(from);
			playlist.add(to, item);
			if (mAdapter!=null) mAdapter.notifyDataSetChanged();
		}
		
		//IMPORTANT:This getters should be used only in playback calls
		public Track getTrack(int index) {
			//mCurrentTrackIndex=index; 	//TODO: This line is possible source of errors. Have to remove it 
			return playlist.get(index);
		}
		
		@Override
		public int getPlaylistSize() {
			return playlist.size();
		}
		
		public Track getNext() {
			return playlist.get(mCurrentTrackIndex++);
		}
		
		public Track getPrev() {
			return playlist.get(mCurrentTrackIndex--);
		}
		
		public Track getFirst() {
			return playlist.get(mCurrentTrackIndex=0);
		}
		
		public Track getLast() {
			return playlist.get(mCurrentTrackIndex=playlist.size()-1);
		}
		
		
		
		public void setCurrentTrackIndex(int i){
			mCurrentTrackIndex = i;
		}
				
		public Track getCurrentTrack() {
			return playlist.get(mCurrentTrackIndex);
		}
		
		public int getCurrentTrackIndex() {
			return mCurrentTrackIndex;
		}
		
		
		public Track[] getTracksArray() {
			Track[] temp_track = new Track[playlist.size()];
			return playlist.toArray(temp_track);
		}
		
		//IMPORTANT:this should be used only in ArrayAdapters. All direct interaction with playlist should be prohibited
		/*public ArrayList<Track> getTracksList() {
			return playlist;
		}*/
		
		@Override
		public ArrayAdapter<Track> getPlayListAdapter(Activity activity, int resource) {
			if (mAdapter==null) 
				mAdapter = new ArrayAdapter<Track>(activity, resource, playlist) {
		    			  @Override
		    			  public View getView(int position, View convertView, ViewGroup parent) {
		    				  View track_view = convertView;
		    				  if (track_view == null) {
		    					  LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
		    					  //LayoutInflater inflater = LayoutInflater.from(getContext());
		    					  track_view = inflater.inflate(R.layout.playlist_item, parent, false);
		    				  }
			    		
		    				  TextView trackName = (TextView)track_view.findViewById(R.id.trackName);
		    				  trackName.setText(getItem(position).getTitle());
		    				  
		    				  return track_view;
		    			  }
			  
				}; 
			return mAdapter;
		}

	}

	
	public class Track {

		private String title;
		private String path;
		private String album;
		private String artist;
		/*
		public Track(String title, String path) {
			this.title = title;
			this.path = path;
		}*/
		
		public Track(String title, String path, String album, String artist) {
			this.title = title;
			this.path = path;
			this.album = album;
			this.artist = artist;
		}
		
		public String getPath() {
			return path;
		}
		
		public String getTitle() {
			return title;
		}
		
		public String getAlbum() {
			return album;
		}
		
		public String getArtist() {
			return artist;
		}
		
		public Track clone() {
			return new Track(title, path, album, artist);
			
		}
	}
	
	
}
	
