package com.notime2wait.simpleplayer.fragments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.notime2wait.simpleplayer.IPlaylist;
import com.notime2wait.simpleplayer.MainActivity;
import com.notime2wait.simpleplayer.MusicData;
import com.notime2wait.simpleplayer.PlaylistDbHelper;
import com.notime2wait.simpleplayer.R;
import com.notime2wait.simpleplayer.MusicData.Track;
import com.notime2wait.simpleplayer.R.id;
import com.notime2wait.simpleplayer.R.layout;

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
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class TrackListFrag extends ListFragment{
	
	private static String LOG_TAG = TrackListFrag.class.getName(); 
	
	private int HEADER_LISTNUM_OFFSET;
	private ArrayAdapter<Track> mAdapter;
	//private ArrayList<Track> tracks;
	private ListIterator<IPlaylist<Track>> mPlaylistHistory;
	private IPlaylist<Track> mPlaylist; //initialization in getTrackListAdapter() method for optimization purposes
	//private int mHomePlaylist; //this is required since the visible playlist could be other than the playing one 
								//while browsing Playlist History we have to check if our current visible list is the same
	//private IPlaylist<Track> mHomePlaylist;
	private MusicData mMusicData;
	//private DragSortController mController;
	private boolean mHistoryButtonFlag = true; //required to reduce unnecessary moves by iterator when switching direction
	
	//header variables
	private View mHeaderView;
	private EditText mPlaylistName;
	private ImageButton mTracklistNext;
	private ImageButton mTracklistPrev;
	private ImageButton mTracklistSave;
	private boolean initialized = false;
	
	

	
		@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMusicData = MainActivity.getMusicData();
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		HEADER_LISTNUM_OFFSET = 0;

		mPlaylistHistory = mMusicData.getPlaylistHistory();
		if (mHeaderView==null) mHeaderView = inflater.inflate(R.layout.tracklist_header, null);
		prepareHeaderView();
		
		DragSortListView listView = (DragSortListView) inflater.inflate(R.layout.dragndrop_frag, container, false);

		//mController = getController(listView);
		listView.setFloatViewManager(getController(listView));
		listView.setOnTouchListener(getController(listView));
		listView.setDragEnabled(true);
		listView.setDropListener(getDropListener());
		listView.setRemoveListener(getRemoveListener());
		return listView;
	}
		
	private boolean prepareHeaderView() {
		
		mPlaylistName = (EditText)mHeaderView.findViewById(R.id.tracklist_title);
		//InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		//imm.showSoftInput(mPlaylistName, InputMethodManager.SHOW_IMPLICIT);
		/*mPlaylistName.setOnFocusChangeListener( new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mPlaylistName.getWindowToken(), 0);	
				}
			}
		});*/
		
		mPlaylistName.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	            mPlaylist.setTitle(s.toString());
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    }); 
		this.mTracklistPrev = (ImageButton) mHeaderView.findViewById(R.id.btn_tracklist_prev);
		this.mTracklistNext = (ImageButton) mHeaderView.findViewById(R.id.btn_tracklist_next);
		this.mTracklistSave = (ImageButton) mHeaderView.findViewById(R.id.btn_tracklist_save);
		
		mTracklistPrev.setOnClickListener( new OnClickListener(){
			 public void onClick(View v) {
				 	//mPlaylistHistory.add(mPlaylist);
			      //show message
				 
				    ((DragSortListView)getListView()).unregisterObserver(mAdapter);
				    if (!mHistoryButtonFlag){
						 mPlaylistHistory.previous();
						 mHistoryButtonFlag = true;
					 }
				 	mPlaylist = mPlaylistHistory.previous();
				 	mMusicData.setHistoryIndex(mMusicData.getHistoryIndex()-1);
				 	//mPlaylistHistory.remove();
				 	if (mTracklistNext.getVisibility() == View.INVISIBLE)
				 		mTracklistNext.setVisibility(View.VISIBLE);
				 	if (!mPlaylistHistory.hasPrevious()) mTracklistPrev.setVisibility(View.INVISIBLE);
				 	

					mPlaylistName.setText(mPlaylist.getTitle());
					mAdapter = getTrackListAdapter();
				    setListAdapter(mAdapter);
				    //mPlaylistName.setText(mAdapter.getCount());
			     }
		});
		
		mTracklistNext.setOnClickListener( new OnClickListener(){
			 public void onClick(View v) {
			      //show message

				 	((DragSortListView)getListView()).unregisterObserver(mAdapter);
				 	if (mHistoryButtonFlag){
						 mPlaylistHistory.next();
						 mHistoryButtonFlag = false;
					 }
				 	mPlaylist = mPlaylistHistory.next();
				 	mMusicData.setHistoryIndex(mMusicData.getHistoryIndex()+1);
				 	if (mTracklistPrev.getVisibility() == View.INVISIBLE)
				 		mTracklistPrev.setVisibility(View.VISIBLE);
				 	if (!mPlaylistHistory.hasNext()) mTracklistNext.setVisibility(View.INVISIBLE);
				 	
					mPlaylistName.setText(mPlaylist.getTitle());
					mAdapter = getTrackListAdapter();
				    setListAdapter(mAdapter);
			     }
		});
		
		mTracklistSave.setOnClickListener( new OnClickListener(){
			 public void onClick(View v) {
				 
				 PlaylistDbHelper dbHelper = mMusicData.getPlaylistDbHelper();
				 SQLiteDatabase db = dbHelper.getWritableDatabase();
				 dbHelper.savePlaylist(dbHelper.getWritableDatabase(), mPlaylist);
			 }
		});
				
		try{
			mTracklistNext.setVisibility(mPlaylistHistory.hasNext()? View.VISIBLE : View.INVISIBLE);
			//if (mMusicData.getHistoryIndex()==mPlaylistHistory.nextIndex()) mTracklistNext.setVisibility(View.INVISIBLE);
			if (mMusicData.getHistoryIndex()==mPlaylistHistory.nextIndex()&&mMusicData.getHistoryIndex()==mMusicData.getHistorySize()-1)
				mTracklistNext.setVisibility(View.INVISIBLE);
			mTracklistPrev.setVisibility(mPlaylistHistory.hasPrevious()? View.VISIBLE : View.INVISIBLE);
			if (mMusicData.getHistoryIndex()==mPlaylistHistory.previousIndex()&&mMusicData.getHistoryIndex()==0)
				mTracklistPrev.setVisibility(View.INVISIBLE);
			//if (mMusicData.getHistoryIndex()==mPlaylistHistory.previousIndex()) mTracklistPrev.setVisibility(View.INVISIBLE);
		} catch (ConcurrentModificationException e) {
			e.printStackTrace();
			mPlaylistHistory = mMusicData.getPlaylistHistory();
			mTracklistNext.setVisibility(mPlaylistHistory.hasNext()? View.VISIBLE : View.INVISIBLE);
			//if (mMusicData.getHistoryIndex()==mPlaylistHistory.nextIndex()) mTracklistNext.setVisibility(View.INVISIBLE);
			if (mMusicData.getHistoryIndex()==mPlaylistHistory.nextIndex()&&mMusicData.getHistoryIndex()==mMusicData.getHistorySize()-1)
				mTracklistNext.setVisibility(View.INVISIBLE);
			mTracklistPrev.setVisibility(mPlaylistHistory.hasPrevious()? View.VISIBLE : View.INVISIBLE);
			if (mMusicData.getHistoryIndex()==mPlaylistHistory.previousIndex()&&mMusicData.getHistoryIndex()==0)
				mTracklistPrev.setVisibility(View.INVISIBLE);
		}
		return true;
	}
	
/*
	private void prepareHistoryIterator() {
		//if (mHomePlaylist !=  mMusicData.getHistoryIndex()) 
		//{
		//	mPlaylist = mMusicData.getCurrentPlaylist();
		//}
		//mHomePlaylist = mMusicData.getHistoryIndex();
		//if (mHomePlaylist==null)
		//	mHomePlaylist = mMusicData.getCurrentPlaylist();
	}*/
	
	private boolean addHeaderView() {
		
		if (mHeaderView==null||mPlaylist==null) return false;
		prepareHeaderView();
		this.getListView().addHeaderView(mHeaderView);
		mPlaylistName.setText(mPlaylist.getTitle());
		HEADER_LISTNUM_OFFSET++;
		initialized = true;
		return true;
}	
	/*
	private boolean isHomePlaylist() {
		return mMusicData.getHistoryIndex()==this.mHomePlaylist;
	}*/
	
			
	  @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//DragSortListView listView = (DragSortListView) getListView(); 
		//listView.setDropListener(getDropListener());
		//listView.setRemoveListener(getRemoveListener());

		if (MainActivity.DEBUG) Log.e("QQQQQQQ", "SSSSSS");
	    mAdapter = getTrackListAdapter();
	    setListAdapter(mAdapter);
	    /*
		SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        listView,
                        new SwipeDismissListViewTouchListener.OnDismissCallback() {
                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                            	
                            		for (int position : reverseSortedPositions) {
                            			int temp_position = position-HEADER_LISTNUM_OFFSET;
                            			
                            			if (temp_position+1<tracks.size()&&mMusicData.playTrack(tracks.get(temp_position+1), temp_position+1)) 
                            						mPlaylist.setCurrentTrackIndex(temp_position);
                            			else if (tracks.size()>1) 
                            				mMusicData.playTrack(tracks.get(temp_position-1), temp_position-1);
                            			else mMusicData.stopMusic();
                            			
                            			mAdapter.remove(mAdapter.getItem(position-HEADER_LISTNUM_OFFSET));
                                		}
                            		mAdapter.notifyDataSetChanged();
                            }
                        });
        listView.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        listView.setOnScrollListener(touchListener.makeScrollListener());*/
	    
	}
	  
	  public DragSortController getController(DragSortListView listView) {
	        // defaults are
	        //   dragStartMode = onDown
	        //   removeMode = flingRight
	        DragSortController controller = new DragSortController(listView);
	        controller.setDragHandleId(R.id.drag_handle);
	        controller.setRemoveEnabled(true);
	        controller.setSortEnabled(true);
	        controller.setDragInitMode(DragSortController.ON_DRAG);
	        controller.setRemoveMode(DragSortController.FLING_REMOVE);
	        return controller;
	    }  						
	  
	  @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		  //v.setBackgroundColor(color.background_light);
			  mMusicData.playTrack(position-HEADER_LISTNUM_OFFSET, mPlaylist);
			  //mHomePlaylist = mMusicData.getHistoryIndex(); 
			  MainActivity.slidingMenu.showContent(true);
		  
	}
	
	private ArrayAdapter<Track> getTrackListAdapter() {
		//TODO: possible source of errors. Have to replace this with a current playlist from the list iterator
		if (mPlaylist == null) 
		{
			mPlaylist = mMusicData.getCurrentPlaylist();
		}
		if (!initialized)
			addHeaderView();
		return mPlaylist.getPlayListAdapter(getActivity(), R.layout.folderlist_item);
		  /*return new ArrayAdapter<Track>(getActivity(),
			        R.layout.folderlist_item, mPlaylist.getTracksList()) {
		    

			  @Override
			  public View getView(int position, View convertView, ViewGroup parent) {
			    View track_view = convertView;
			    if (track_view == null) {
			       LayoutInflater inflater = getActivity().getLayoutInflater();
			    	//LayoutInflater inflater = LayoutInflater.from(getContext());
			       track_view = inflater.inflate(R.layout.playlist_item, parent, false);
			    }
			    		
			    TextView trackName = (TextView)track_view.findViewById(R.id.trackName);
	    		trackName.setText(getItem(position).getTitle());

			    return track_view;
			  }
			  
		    }; */
	}
	 

	  
	  
	private DragSortListView.DropListener getDropListener() {
		return new DragSortListView.DropListener() {
		            @Override
		            public void drop(int from, int to) {
		                //Track item = mAdapter.getItem(from-HEADER_LISTNUM_OFFSET);

		                mPlaylist.move(from, to);
		                
		                /*
		                mPlaylist.remove(from-HEADER_LISTNUM_OFFSET);
		                //for (Track t:mPlaylist.getTracksList()) Log.e("Before remove", t.getTitle());
		                mPlaylist.add(to-HEADER_LISTNUM_OFFSET, item);
						*/
		                //for (Track t:mPlaylist.getTracksList()) Log.e("after insert", t.getTitle());
		                /*
		                mAdapter.remove(item);
		                for (Track t:tracks) Log.e("Before remove", t.getTitle());
		                mAdapter.insert(item, to-HEADER_LISTNUM_OFFSET);
		                for (Track t:tracks) Log.e("after insert", t.getTitle());
		                */
		                
		            }
		        };
	}

	private DragSortListView.RemoveListener getRemoveListener() {
			
		return new DragSortListView.RemoveListener() {
		    @Override
		    public void remove(int position) {

		    	int currentPlayingTrack = mPlaylist.getCurrentTrackIndex();

		    	if (MainActivity.DEBUG) Log.e(LOG_TAG, "remove pos:"+position+"current pos:"+currentPlayingTrack);
		    	mPlaylist.remove(position); //changes current track index in its method declaration
		    	//mAdapter.notifyDataSetChanged();
		    	if (!mMusicData.isHomePlaylist()) return;
		    	if (mPlaylist.getCurrentTrackIndex()==-1) {
		    		mMusicData.stopMusic();
		    		return;
		    	}
		    	if (position==currentPlayingTrack&&mMusicData.isPlaying())
		    		mMusicData.playTrack(mPlaylist.getCurrentTrackIndex(), mPlaylist);
		    	if (MainActivity.DEBUG) Log.e(LOG_TAG, "remove pos:"+position+"current pos:"+currentPlayingTrack);
                /*int temp_position = position-HEADER_LISTNUM_OFFSET;      			
                mAdapter.remove(mAdapter.getItem(position-HEADER_LISTNUM_OFFSET));
                mAdapter.notifyDataSetChanged();
                
    			if (!mMusicData.isPlaying()) return;
                if (temp_position+1<tracks.size()&&mMusicData.playTrack(tracks.get(temp_position+1), temp_position+1)) 
                				mPlaylist.setCurrentTrackIndex(temp_position);
                else if (tracks.size()>1) 
                				mMusicData.playTrack(tracks.get(temp_position-1), temp_position-1);
                else mMusicData.stopMusic();*/
                }
		            
		};
	}
	
	  
	  	@Override
		  public void onDestroyView() {
	  		super.onDestroyView();
			  setListAdapter(null);
			  initialized = false;
			  MainActivity.handleUndoAction(null); //hide any popups produced by this fragment
			  mPlaylist = null;
			  mHistoryButtonFlag = true;
			  //mHomePlaylist = null;
		  }
}

