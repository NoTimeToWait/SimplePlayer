package com.notime2wait.simpleplayer.fragments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.notime2wait.simpleplayer.MainActivity;
import com.notime2wait.simpleplayer.MusicData;
import com.notime2wait.simpleplayer.PlaylistDbHelper;
import com.notime2wait.simpleplayer.R;
import com.notime2wait.simpleplayer.SwipeDismissListViewTouchListener;
import com.notime2wait.simpleplayer.MusicData.Track;
import com.notime2wait.simpleplayer.PlaylistDbHelper.TracklistEntry;
import com.notime2wait.simpleplayer.R.id;
import com.notime2wait.simpleplayer.R.layout;
import com.notime2wait.simpleplayer.SwipeDismissListViewTouchListener.OnDismissCallback;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.R.color;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class PlaylistsFrag extends BackHandledListFragment implements LoaderCallbacks<Cursor>{
	
	private static String LOG_TAG = PlaylistsFrag.class.getName(); 
	
	private int HEADER_LISTNUM_OFFSET = 0;
	private CursorAdapter playlistAdapter;
	private MusicData mMusicData;
	private String[] playlists;
	private Track[] openedPlaylistTracks;
	private String openedPlaylist;
	private boolean isPlaylistView = true;
	private View headerView;
	

		@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMusicData = MainActivity.getMusicData();
		
		
	}
			
	  @Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    //Set<String> set = folderlist.keySet();
	    LoaderManager lm = getLoaderManager();
	    lm.initLoader(this.getId(), null, this);
	    headerView = new TextView(getActivity());
	    ((TextView)headerView).setText("HEADER");
	   // this.getListView().addHeaderView(headerView);
	   // this.getListView().addHeaderView(headerView);
	    getLoaderManager().getLoader(this.getId()).forceLoad();
	    playlistAdapter = getplaylistAdapter();
	    setListAdapter(playlistAdapter);
	    setListShownNoAnimation(true);
	    ListView listView = this.getListView();
		SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        listView,
                        new SwipeDismissListViewTouchListener.OnDismissCallback() {
                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                            	if (isPlaylistView) {
                            		PlaylistDbHelper dbHelper = MainActivity.getMusicData().getPlaylistDbHelper();
                        			SQLiteDatabase db = dbHelper.getReadableDatabase();
                            		for (int position : reverseSortedPositions) {
                            			
                            			String playlistName = ((TextView)getViewByPosition(listView, position).findViewById(R.id.folderName)).getText().toString();
                            			Cursor cursor = dbHelper.getTracklist(db, playlistName);
                       		         	
                            			mMusicData.addTracksToPlaylist(getTracks(cursor));
                            		}
                            	}
                            	else 
                            		for (int position : reverseSortedPositions) {
                                        mMusicData.addTrackToPlaylist(openedPlaylistTracks[position]);
                                		}
                                //mAdapter.notifyDataSetChanged();
                            	
                            }
                        });
        listView.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        listView.setOnScrollListener(touchListener.makeScrollListener());
	    
	}
	  
	  						
	  
	  @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		  if (isPlaylistView) {
			  isPlaylistView = false;
			  openedPlaylist = ((TextView)v.findViewById(R.id.folderName)).getText().toString();
			  getLoaderManager().getLoader(this.getId()).forceLoad();
		  }
		  //TODO:
		  else {
			  
			  openedPlaylistTracks = getTracks(playlistAdapter.getCursor());
			  

				//if (MainActivity.DEBUG) Log.e(LOG_TAG, "ssssss"+position);
			  mMusicData.playTracks(openedPlaylist, position, openedPlaylistTracks);
			  //TODO
			  MainActivity.slidingMenu.showContent(true);
		  }
		  //v.setBackgroundColor(color.background_light);
		  /*if (isPlaylistView) {
			  	  openedPlaylist = playlists[position-HEADER_LISTNUM_OFFSET];
			  	  if (MainActivity.DEBUG) Log.d(LOG_TAG, "Position "+playlists[position-HEADER_LISTNUM_OFFSET]);
				  //tracklistAdapter = getTrackListAdapter(openedPlaylist);
				  //setListAdapter(tracklistAdapter);
				  isPlaylistView = false;
		  }
		  else {
			  mMusicData.playTrack(openedPlaylist, position, openedPlaylistTracks.length);
			  MainActivity.slidingMenu.showContent(true);
		  }*/
		  
	}
	
	private Track[] getTracks(Cursor cursor) {
		  Track[] tracks = new Track[cursor.getCount()];
		  if (cursor.moveToFirst()) {
				int i=0;
				String music_data, trackname, album, artist;
				
				do {
					music_data = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_PATH));

					trackname = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_TITLE));
					album = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_ALBUM));
					artist = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_ARTIST));
					tracks[i] = mMusicData.new Track(trackname, music_data, album, artist);
					i++;
				} while (cursor.moveToNext());
		}
		return tracks;
	}
	
	private View getViewByPosition(ListView listView, int pos) {
	    final int firstListItemPosition = listView.getFirstVisiblePosition();
	    final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

	    if (pos < firstListItemPosition || pos > lastListItemPosition ) {
	        return listView.getAdapter().getView(pos, null, listView);
	    } else {
	        final int childIndex = pos - firstListItemPosition;
	        return listView.getChildAt(childIndex);
	    }
	}
	  
	private CursorAdapter getplaylistAdapter() {
		
		//String[] from = new String[] { PlaylistDbHelper., DB.COLUMN_TXT };
	    //int[] to = new int[] { R.id.ivImg, R.id.tvText };
		//return new SimpleCursorAdapter(getActivity(), R.layout.folderlist_item, null, playlists, null, 0);
	    return new CursorAdapter(getActivity(), null, 0) {
	    	@Override
	        public void bindView(View view, Context context, Cursor cursor) {
	    		TextView name = (TextView)view.findViewById(R.id.folderName);
	    		if (isPlaylistView) {
	    			
	    			//TextView folderPath = (TextView)view.findViewById(R.id.folderPath);
	    			name.setText(cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.PlaylistEntry.COLUMN_TITLE)));
	    			//folderPath.setText(folder_item);
	    		}
	    		else {
	    			// TODO Add plalist switch
	    			name.setText(cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_TITLE)));
	    		}
	        }

	        @Override
	        public View newView(Context context , Cursor cursor, ViewGroup parent) {
	            // TODO Auto-generated method stub
	            //LayoutInflater inflater = LayoutInflater.from(context);
	            //View view = inflater.inflate(R.layout.message_row_view, viewGroup ,false);
	        	LayoutInflater inflater = getActivity().getLayoutInflater();
		    	//LayoutInflater inflater = LayoutInflater.from(getContext());
		    	View playlistView = inflater.inflate(R.layout.folderlist_item, parent, false);
	            return playlistView;
	        }
	    };
		 
	}
	
		public String openedPlaylist() {
			if (isPlaylistView) return null;
			else return openedPlaylist;
		}

	  	@Override
		  public void onDestroyView() {
	  		super.onDestroyView();
			  setListAdapter(null);
			  isPlaylistView = true;
			  getLoaderManager().getLoader(this.getId()).forceLoad();
		  }

	@Override
	public String getTagText() {
		return "PlaylistTab";
	}

	@Override
	public boolean onBackPressed() {
//TODO: add back handling
		if (!isPlaylistView && MainActivity.slidingMenu.isMenuShowing()) {
			/* returns back to menu from the front slide - deprecated
			 * if (!MainActivity.slidingMenu.isMenuShowing()) {
				MainActivity.slidingMenu.showMenu();
				return true;
			} */
			isPlaylistView = true;
			getLoaderManager().getLoader(this.getId()).forceLoad();
			return true; //back event is consumed
		}
		return false;	//back event is not consumed
	}
	
	public static class PlaylistCursorLoader extends CursorLoader {
		
		PlaylistsFrag fragment;
		public PlaylistCursorLoader(Context context, PlaylistsFrag frag) {
			super(context);
			this.fragment=frag;
		}
		
		@Override
		public Cursor loadInBackground() {
			PlaylistDbHelper dbHelper = MainActivity.getMusicData().getPlaylistDbHelper();
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			//if (MainActivity.DEBUG) Log.e("SSSSSSSSS", ""+fragment.openedPlaylist());
		         Cursor cursor = (fragment.openedPlaylist() == null)? dbHelper.getPlaylists(db) : dbHelper.getTracklist(db, fragment.openedPlaylist());
		         //if (cursor != null) {
		         //    cursor.getCount();
		         //    this.registerContentObserver(cursor, mObserver);
		         //}
		         return cursor;
		}
		
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new PlaylistCursorLoader(getActivity(), this);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
		if (loader.getId() == this.getId())
			playlistAdapter.swapCursor(newCursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		playlistAdapter.swapCursor(null);
	}
}

