package com.notime2wait.simpleplayer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.notime2wait.simpleplayer.MusicService.MusicServiceObserver;
import com.notime2wait.simpleplayer.UndoBarController.Undoable;
import com.notime2wait.simpleplayer.fragments.IBackHandledFragment;
import com.notime2wait.simpleplayer.fragments.IBackHandledFragment.BackHandlerInterface;
import com.notime2wait.simpleplayer.visualization.IVisuals;
import com.notime2wait.simpleplayer.visualization.WaveformUtils;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity 
		implements SeekBar.OnSeekBarChangeListener, BackHandlerInterface{
	//implements AdapterView.OnItemClickListener, StickyListHeadersListView.OnHeaderClickListener {

    //private TestBaseAdapter mAdapter;
	private static String LOG_TAG = MainActivity.class.getName(); 
	public static String RECENT_PLAYLIST = "RecentLast";
	public static String BACKUP_STATE = "backup_state";
	private static int PARCEL_VER = 1;
	private static int PROGRESS_UPDATE_TIME = 1000;
	//private static MusicData mMusicData = new MusicData();
	//private static WaveformUtils mVisuals;
	
	public static float density; 
	public static boolean DEBUG = true;
	public static SlidingMenu slidingMenu;
	//TODO: have to check if there are any possible errors that will require creating a new instance of the MediaPlayer. Thus, removing "final"
		// property will require checking SessionId and properly updating it wherever it is used.
    //private static final MediaPlayer mPlayer = new MediaPlayer();
    private static UndoBarController mUndoBarController;
    private WaveformUtils mVisuals;
    private Rect mWaveformBounds;
    
	//private ImageButton mBtnPlay;
    //private ImageButton mBtnNext;
    //private ImageButton mBtnPrev;
    //private ButtonWheelView mBtnWheel;
    private SeekBar mProgressBar;
    private TextView mTitle;
    private TextView mTitleDescr;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    private BgView mBackground;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();
    // fragment to propagate events, e.g. back button pressed
    private IBackHandledFragment mFragment;
	
    protected static FragmentTabHost mPreferencesTabs;
    protected static FragmentTabHost mTracklistTabs;
    
	private static MusicService mMusicService;
	private ServiceConnection mMusicServiceCon = new ServiceConnection(){

		public void onServiceConnected(ComponentName name, IBinder binder) {
			mMusicService = ((MusicService.ServiceBinder)binder).getService();
			prepare();
			mMusicService.registerObserver(new MusicServiceObserver() {
				public void update(Bundle extras, int source) {
					if ((source&MusicService.SOURCE_TRACK_CHANGED)==MusicService.SOURCE_TRACK_CHANGED && extras.containsKey("track"))
						updateUIStart((Track)extras.getParcelable("track"));
				}
		    });
		}

		public void onServiceDisconnected(ComponentName name) {
			mMusicService = null;
		}
	};
		
	public static MusicData getMusicData() {
		return mMusicService.getMusicData();
	}
	
	public static int getSessionId() {
		return mMusicService.getSessionId();
	}
	
	public static void setPreferencesTabHost(FragmentTabHost tabHost) {
		mPreferencesTabs = tabHost;
	}
	
	public static void setTracklistTabHost(FragmentTabHost tabHost) {
		mTracklistTabs = tabHost;
	}
	
	/*protected void onSaveInstanceState(Bundle state) {
	    super.onSaveInstanceState(state);
	    state = writeStateBundle(state);
	  }
	
	private Bundle writeStateBundle(Bundle state) {
		IPlaylist<Track> playlist = getMusicData().getCurrentPlaylist();
	    //state.putInt("progress", mProgressBar.getProgress());
	    state.putInt("progress", mMusicService.getMediaPlayer().getCurrentPosition());
	    //state.putCharSequence("duration", songTotalDurationLabel.getText());
	    //state.putCharSequence("current_duration", songCurrentDurationLabel.getText());
	    state.putCharSequence("title", mTitle.getText());
	    //state.putCharSequence("title_descr", mTitleDescr.getText());
	    state.putIntArray("heights", mVisuals.getHeights());
	    state.putString("playlist_title", playlist.getTitle());
	    state.putParcelableArray("tracklist", playlist.getTracksArray());
	    state.putInt("tracknum", playlist.getCurrentTrackIndex());
	    return state;
	}*/
	
	private void writeStateParcel(Parcel state, int flags) {
		IPlaylist<Track> playlist = getMusicData().getCurrentPlaylist();
	    //state.putInt("progress", mProgressBar.getProgress());
		state.writeInt(PARCEL_VER);
	    state.writeInt(mMusicService.getCurrentPosition());
	    //state.putCharSequence("duration", songTotalDurationLabel.getText());
	    //state.putCharSequence("current_duration", songCurrentDurationLabel.getText());
	    state.writeString(mTitle.getText().toString());
	    //state.putCharSequence("title_descr", mTitleDescr.getText());
	    state.writeString(playlist.getTitle());
	    state.writeInt(playlist.getCurrentTrackIndex());
	    state.writeTypedArray(playlist.getTracksArray(), flags);
	    state.writeIntArray(mVisuals.getHeights()==null? new int[0] : mVisuals.getHeights());
	    if (mVisuals.getHeights()==null) Log.e(LOG_TAG, "MVISUALS null heights");
	    else Log.e(LOG_TAG, "MVISUALS"+ mVisuals.getHeights().length);
	}
	/*
	public static IVisuals getVisuals() {
		return mVisuals;
	}*/
	//call only from onServiceConnected method
	public void prepare() {

    	Log.e("", "Created");
		slidingMenu = (SlidingMenu) findViewById(R.id.slidingmenu);
        slidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
        slidingMenu.setSecondaryMenu(R.layout.right_slide);
        //slidingMenu.showMenu(false);
        slidingMenu.setOnCloseListener(new OnCloseListener() {
			@Override
			public void onClose() {
				mUndoBarController.handleUndoToken(null); // hide any popups on slide
			}
        });
        
        mBackground = (BgView) findViewById(R.id.background);
        ButtonWheelView mBtnWheel = (ButtonWheelView) findViewById(R.id.button_wheel);
        mBtnWheel.addHeaderView(getWheelButton(0, mBtnWheel));
        for (int i=1; i<6; i++) 
	    	 mBtnWheel.addView(getWheelButton(i, mBtnWheel), i);
	    	
        ViewGroup.LayoutParams lp = mBtnWheel.getLayoutParams();
        mBtnWheel.layout(mBtnWheel.getLeft(), mBtnWheel.getTop(), mBtnWheel.getRight(), mBtnWheel.getBottom());
        mProgressBar = (SeekBar) findViewById(R.id.music_progressbar);
        mTitle = (TextView) findViewById(R.id.track_label);
        mTitleDescr = (TextView) findViewById(R.id.track_label_descr);
        mTitleDescr.setSelected(true);
        songCurrentDurationLabel = (TextView) findViewById(R.id.current_duration);
        songTotalDurationLabel = (TextView) findViewById(R.id.total_duration);
        mUndoBarController =  new UndoBarController(findViewById(R.id.undobar));
        
        ImageButton btnPlay = getButtonPlay();
        ImageButton btnPrev = getButtonPrev();
        ImageButton btnNext = getButtonNext();
        mBackground.setDefaultBackground();
    	mProgressBar.setProgressDrawable(null);
    	mProgressBar.setOnSeekBarChangeListener(this); // Important
    	if (mVisuals==null)  {
    		//height is 45dp because we need to calculate hate of the upper side of the waveform (which is 2/3 of the total height = 67dp) 
    		mVisuals = new WaveformUtils(getResources().getDisplayMetrics().widthPixels-(int)(32*density), (int) (45*density), this);
            mVisuals.setOnVisualsUpdateListener(new IVisuals.OnVisualsUpdateListener() {

				@Override
				public void onVisualsUpdate(Bitmap cachedBitmap) {
					if (mWaveformBounds==null) 
						mWaveformBounds = new Rect( 0, 
													0,
													getResources().getDisplayMetrics().widthPixels-(int)(32*density), 
													(int) (67*density));
					//ViewGroup.LayoutParams lp = mProgressBar.getLayoutParams();
					//lp.height = mWaveformBounds.height();
					//mProgressBar.setLayoutParams(lp);
					BitmapDrawable drawable = new BitmapDrawable(getResources(), cachedBitmap);
					drawable.setBounds(mWaveformBounds);

					Log.e("", "mBOUNDS "+drawable.getBounds().right+" "+drawable.getBounds().bottom);
					/*ViewGroup.LayoutParams lp =  mProgressBar.getLayoutParams();
		             lp.width = drawable.getBounds().width();
		             mProgressBar.setLayoutParams(lp);*/
		             /*if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) 
		            	 mProgressBar.setBackgroundDrawable(drawable);
		             else mProgressBar.setBackground(drawable);*///drawable.getBounds();
					 
					 mProgressBar.setProgressDrawable(drawable);
					 Rect bounds = mProgressBar.getProgressDrawable().getBounds();
					 Log.e("", "BOUNDS "+bounds.right+" "+bounds.bottom);
		             
		             Log.e("" , "mProgressBar.width " + mProgressBar.getWidth() + " mProgressBar.h " + mProgressBar.getHeight());
				}
           	 
            });
    	}
    	
    	boolean parcelObtained = false;
    	Parcel p = Parcel.obtain();
    	try {
    			FileInputStream inputStream = openFileInput(BACKUP_STATE);
    			byte[] array = new byte[(int) inputStream.getChannel().size()];
    			if (inputStream.read(array, 0, array.length)==-1) throw new IOException("End reached while trying to read parcel");
    			inputStream.close();
    			p.unmarshall(array, 0, array.length);
    			p.setDataPosition(0);
    			//mState = p.readBundle(Track.class.getClassLoader());
    			parcelObtained = true;
    	} catch (Exception e) {
    			Log.w(LOG_TAG, "Couldn't open temporary backup file"+e.getMessage());
    	} 
    	
    	if (parcelObtained && p.readInt()==PARCEL_VER) {
    		/*if (mMusicService!=null) {
    			String title = mState.getCharSequence("title").toString();
        		if (!title.equals(getMusicData().getCurrentTrack().getTitle())) {
        			startService(new Intent(this, MusicService.class));
        		    bindService(new Intent(this, MusicService.class),
        		    			mMusicServiceCon,Context.BIND_AUTO_CREATE);
        			return;
        		}
    		}*/
    	    //songTotalDurationLabel.setText(mState.getCharSequence("duration"));
    	   // mTitle.setText(mState.getCharSequence("title"));
    	    //mTitleDescr.setText(mState.getCharSequence("title_descr"));
    	    //mVisuals.calculateWaveform(savedState.getIntArray("heights"));
    	    
    	    //if (mMusicService==null) {
        	//    songCurrentDurationLabel.setText(mState.getCharSequence("current_duration"));
        	//    mProgressBar.setProgress(mState.getInt("progress"));
    		// }
    	
    		Track track;
    		
    		int playerPosition = p.readInt();
    		String trackTitle = p.readString();
    		String playlistTitle = p.readString();
    		int tracknum = p.readInt();
    		Track[] tracks = p.createTypedArray(Track.CREATOR);
    		    		
    		if (getMusicData().getCurrentPlaylist().getPlaylistSize()==0 
    				|| getMusicData().getCurrentPlaylist().getTitle().equals(getMusicData().getFavoritesTitle()) ) {
    			if (tracks==null || tracks.length==0) return;
    			getMusicData().prepareTracks(playlistTitle, tracknum, tracks, true);
    			mMusicService.seekTo(playerPosition);
    		
    		}
        /*if (getMusicData().getCurrentPlaylist().getPlaylistSize()==0) 
        {
        	PlaylistDbHelper dbHelper = getMusicData().getPlaylistDbHelper();
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			//if (MainActivity.DEBUG) Log.e("SSSSSSSSS", ""+fragment.openedPlaylist());
		    Cursor cursor = dbHelper.getTracklist(db, RECENT_PLAYLIST);
		    Track[] tracks = new Track[cursor.getCount()];
			  if (cursor.moveToFirst()) {
					int i=0;
					String music_data, trackname, album, artist, albumArt;
					
					do {
						music_data = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_PATH));

						trackname = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_TITLE));
						album = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_ALBUM));
						artist = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_ARTIST));
						albumArt = cursor.getString(cursor.getColumnIndex(PlaylistDbHelper.TracklistEntry.COLUMN_ART));
						tracks[i] = new Track(trackname, music_data, album, artist, albumArt);
						i++;
					} while (cursor.moveToNext());
			}
			getMusicData().playTracks(RECENT_PLAYLIST, 0, tracks);
			mMusicService.pause();
        }*/
    	
    		try {
        		track = getMusicData().getCurrentTrack();
        		//TODO: move to UpdateUI
                mBackground.setAlbumImage(track.getAlbumArt(true));
                //mBackground.invalidate();
                mTitle.setText(track.getTitle());
                mTitleDescr.setText(track.getArtist()+" - "+track.getAlbum());
                mProgressBar.setMax(mMusicService.getDuration()/1000);
                mProgressBar.setProgress(mMusicService.getCurrentPosition()/PROGRESS_UPDATE_TIME);
                
                songTotalDurationLabel.setText(milliSecondsToTimer(mMusicService.getDuration()));
        		songCurrentDurationLabel.setText(milliSecondsToTimer(mMusicService.getCurrentPosition()));
                mHandler.postDelayed(updateProgressBarTask, 1);
                                 //mVisuals.setTrack(track);
                if (trackTitle.equals(track.getTitle())) {
                	Log.e("", "From Int Array");
                	mVisuals.calculateWaveform(p.createIntArray());
                }
                else mVisuals.calculateWaveform(track);
                
    		} catch (IllegalArgumentException e) {
                e.printStackTrace();
    		} catch (IllegalStateException e) {
    			e.printStackTrace();
    		} 
    	}
    	parcelObtained = false;
    	p.recycle();
        //mState = null;
        mMusicService.setStarted();
	}

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        density = getResources().getDisplayMetrics().density;
        getWindow().setBackgroundDrawable(null);
		//getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
	    //getActionBar().hide();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_main);
	    setVolumeControlStream(AudioManager.STREAM_MUSIC);
	    
    	//mState = savedState;
	    
    	startService(new Intent(this, MusicService.class));
	    bindService(new Intent(this, MusicService.class),
	    			mMusicServiceCon,Context.BIND_AUTO_CREATE);

	    

	    
        //mMusicData.init(this);
        
        
        //StickyListAdapter mAdapter = new StickyListAdapter(this);

       // StickyListHeadersListView stickyList = (StickyListHeadersListView) findViewById(R.id.list);
        //stickyList.setOnItemClickListener(this);
        //stickyList.setOnHeaderClickListener(this);

//		mStickyList.addHeaderView(inflater.inflate(R.layout.list_header, null));
//		mStickyList.addFooterView(inflater.inflate(R.layout.list_footer, null));
       // stickyList.setEmptyView(findViewById(R.id.empty));

        //stickyList.setDrawingListUnderStickyHeader(true);
       // stickyList.setAreHeadersSticky(true);

        //stickyList.setAdapter(mAdapter);
        
        
        //bgImageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        //FrameLayout l = (FrameLayout) findViewById(R.layout.activity_main);
        //System.out.println(l);
        //System.out.println(bgImageView);
        //.addView(bgImageView);
        
        //ImageView iv = (ImageView) findViewById(R.id.main_background);
        //iv.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.simpleplayer_bg),480,800,true));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public static boolean handleUndoAction(Undoable undoToken) {
    	//return mUndoBarController.handleUndoToken(undoToken);
    	return mUndoBarController.handleUndoToken(undoToken);
    }
    
    private String milliSecondsToTimer(int milliseconds){
        String timerString = "";
 
           int hours = (int)( milliseconds / (1000*60*60));
           int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
           int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);
           
           if(hours > 0) timerString = hours + ":";
           timerString += minutes+":";
           timerString += seconds < 10 ? "0" + seconds : seconds;
        return timerString;
    }
 
    public float getProgressPercentage(long currentDuration, long totalDuration){
        return currentDuration/totalDuration;
    }
 
    
    public int progressToTimer(int progress, int totalDuration) {
        int currentDuration = 0;
        totalDuration = (int) (totalDuration / 1000);
        currentDuration = (int) ((((double)progress) / 100) * totalDuration);
 
        // return current duration in milliseconds
        return currentDuration * 1000;
    }
    
    public void updateUIStop() {
    	//TODO: add here bg image, waveform and song title removal 
   	 	//mBtnPlay.setImageResource(R.drawable.btn_play_icn);
    }
    
    public void  updateUIStart(Track track){
        // Play song
        try {
            mBackground.setAlbumImage(track.getAlbumArt(true));
            mTitle.setText(track.getTitle());
            mTitleDescr.setText(track.getArtist()+" - "+track.getAlbum());
            
            // Changing Button Image to pause image
            //btnPlay.setImageResource(R.drawable.btn_pause);
 
            // set Progress bar values from 0 to track_length_in_seconds
            mProgressBar.setProgress(0);
            mProgressBar.setMax(mMusicService.getDuration()/1000);
            //if (mMusicService.isPlaying()) mBtnPlay.setImageResource(R.drawable.btn_pause_icn);
            songTotalDurationLabel.setText(milliSecondsToTimer(mMusicService.getDuration()));
    		songCurrentDurationLabel.setText(milliSecondsToTimer(0));
            //mProgressBar.setBackground(mVisuals.)
            // Updating progress bar
            
             
            mHandler.postDelayed(updateProgressBarTask, PROGRESS_UPDATE_TIME);
            
             //mVisuals.setTrack(track);
             /*WaveformUtils mVisuals = new WaveformUtils(getResources().getDisplayMetrics().widthPixels-(int)(16*density), (int) (45*density), this);
             mVisuals.setOnVisualsUpdateListener(new IVisuals.OnVisualsUpdateListener() {

            	 @Override
 				public void onVisualsUpdate(Bitmap cachedBitmap) {
 					BitmapDrawable drawable = new BitmapDrawable(getResources(), cachedBitmap);
 		             if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) 
 		            	 mProgressBar.setBackgroundDrawable(drawable);
 		             else mProgressBar.setBackground(drawable);
 				}
            	 
             });*/
             mVisuals.calculateWaveform(track);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } 
    }
    
    private Runnable updateProgressBarTask = new Runnable() {
        public void run() {
        	if (mMusicService.isPlaying()) {
        		songTotalDurationLabel.setText(milliSecondsToTimer(mMusicService.getDuration()));
        		songCurrentDurationLabel.setText(milliSecondsToTimer(mMusicService.getCurrentPosition()));
        		mProgressBar.setProgress(mMusicService.getCurrentPosition()/PROGRESS_UPDATE_TIME);
        	}
            mHandler.postDelayed(this, PROGRESS_UPDATE_TIME);
        }
     };
    /*
     public void stopMusic() {
    	 //TODO: add here bg image, waveform and song title removal 
    	 mPlayer.stop();
    	 mBtnPlay.setImageResource(R.drawable.btn_play_icn);
     }*/
     
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // remove message Handler from updating progress bar
        mHandler.removeCallbacks(updateProgressBarTask);
    }
    
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(updateProgressBarTask);
        //if (MainActivity.DEBUG) Log.e(LOG_TAG, ""+seekBar.getProgress());
        // forward or backward to certain seconds
        
        mMusicService.seekTo(seekBar.getProgress()*PROGRESS_UPDATE_TIME);
        songCurrentDurationLabel.setText(milliSecondsToTimer(mMusicService.getCurrentPosition()));
        // update timer progress again
        mHandler.postDelayed(updateProgressBarTask, 1);
    }
    

	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}
	
	/*
    //TODO:finish these methods
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}*/
    
	private View getWheelButton(int index, ViewGroup btnWheel) {
		LayoutInflater inflater = getLayoutInflater();
    	View view = inflater.inflate(R.layout.wheel_item, btnWheel, false);
    	view.setVisibility(View.VISIBLE);
    	switch (index) {
    		case 5:
    			((ImageView)view.findViewById(R.id.inner_icon)).setImageResource(R.drawable.fav_icon_small);
    			view.setOnClickListener(new OnClickListener() {
    				@Override
    				public void onClick(View v) {
    	    			getMusicData().fromToFavorites();
    	    		}
    	    	});
    			break;
    		case 4:
    			((ImageView)view.findViewById(R.id.inner_icon)).setImageResource(R.drawable.pref_icon_small);
    			view.setOnClickListener(new OnClickListener() {
    				@Override
    				public void onClick(View v) {
    					mPreferencesTabs.setCurrentTabByTag("preferences");
    					slidingMenu.showSecondaryMenu();
    				}
    	    	});
    			break;
    		case 3:
    			((ImageView)view.findViewById(R.id.inner_icon)).setImageResource(R.drawable.play_icon_small);
    			view.setOnClickListener(new OnClickListener() {
    				@Override
    				public void onClick(View v) {
    					slidingMenu.showMenu();
    				}
    			});
    		break;
    		case 1:
    			((ImageView)view.findViewById(R.id.inner_icon)).setImageResource(R.drawable.repeat_icon_small);
    		break;
    		case 0:
    			((ImageView)view.findViewById(R.id.inner_icon)).setImageResource(R.drawable.open_icon_small);
    		break;
    	}
    	return view;
	}
	
    private ImageButton getButtonNext(){

        ImageButton mBtnNext = (ImageButton) findViewById(R.id.btn_next);
        mBtnNext.setOnClickListener(new View.OnClickListener() {
    		 
            @Override
            public void onClick(View arg0) {
            	Intent intent = new Intent(MusicService.ACTION_NEXT);
            	LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
            	
            	//mMusicService.playNext();
            	
            }
        });
        return mBtnNext;
    }
    
    private ImageButton getButtonPrev(){

        ImageButton mBtn = (ImageButton) findViewById(R.id.btn_prev);
        mBtn.setOnClickListener(new View.OnClickListener() {
    		 
            @Override
            public void onClick(View arg0) {
            	Intent intent = new Intent(MusicService.ACTION_PREV);
            	LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
            	
            	//mMusicService.playPrevious();
            	
            }
        });
        return mBtn;
    }
    
    private ImageButton getButtonPlay(){

        final ImageButton mBtn = (ImageButton) findViewById(R.id.btn_play);
        mBtn.setBackgroundResource(R.drawable.btn_play);
        mBtn.setImageResource((mMusicService==null||!mMusicService.isPlaying())? R.drawable.btn_play_icn : R.drawable.btn_pause_icn);
        
        mMusicService.registerObserver(new MusicServiceObserver() {
			public void update(Bundle extras, int source) {
				mBtn.setImageResource((source&MusicService.SOURCE_MUSIC_PLAY)==0?  R.drawable.btn_play_icn : R.drawable.btn_pause_icn);
			}
	    });
        
        mBtn.setOnClickListener(new View.OnClickListener() {
 
            @Override
            public void onClick(View v) {
                if(getMusicData().getCurrentPlaylist().getPlaylistSize()==0) return; 
                if (!mMusicService.requestAudioFocus()) return;
                
                //DO NOT DELETE
                //mBtn.setImageResource("pause".equals(mBtn.getContentDescription())? R.drawable.btn_play_icn : R.drawable.btn_pause_icn);
                //mBtn.setContentDescription("pause".equals(mBtn.getContentDescription())? "play" : "pause");
                Intent intent = new Intent(MusicService.ACTION_PLAY);
                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
                
                		//mMusicService.pause();
                    	
                    	//if (DEBUG) Log.e("WWWWW", "PPPPP");
                        // Changing button image to play button
                        //mBtn.setBackgroundResource(R.drawable.btn_play);
                		//Intent intent = new Intent(MusicService.ACTION_PLAY);
                		//LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
                		//mMusicService.start();/
                
            }
        });
        return mBtn;
    }
    
    
    @Override
    public void onBackPressed() {

    	//TODO: check this statement logic
        if ((mFragment == null || !mFragment.onBackPressed()) && !showFrontSlide()) {
            // Selected fragment did not consume the back press event and front slide is currently visible then
            super.onBackPressed();
        }
    }
    
    public boolean showFrontSlide() {
    	if (slidingMenu.isMenuShowing()) {
    		slidingMenu.showContent();
    		return true;
    	}
    	return false;
    }
    
    @Override
	public void setSelectedFragment(IBackHandledFragment backHandledFragment) {
		this.mFragment = backHandledFragment;
		
	}

    
    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.restore:
                mAdapter.restore();
                return true;
            case R.id.update:
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.clear:
                mAdapter.clear();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Toast.makeText(this, "Item " + position + " clicked!",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onHeaderClick(StickyListHeadersListView l, View header,
                              int itemPosition, long headerId, boolean currentlySticky) {
        Toast.makeText(this, "Header " + headerId + " currentlySticky ? " + currentlySticky,
                Toast.LENGTH_SHORT).show();
    }*/
    /*
    public void onDestroyView() {
		  stopMusic();
	  }

	*/

    @Override
    protected void onResume() {
        super.onResume();
        if (mMusicService!=null)
        	mHandler.postDelayed(updateProgressBarTask, PROGRESS_UPDATE_TIME);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mMusicService!=null) {
        	if (mMusicService.isPlaying())
        		if (!mMusicService.isForeground()) mMusicService.startForegroundService();
        	if (!mMusicService.isPlaying()) {
        		mMusicService.stopForeground(true);
        		//The service will be alive while it has binding
        		mMusicService.stopSelf();//stopService(new Intent(this, MusicService.class));
        	}
        }
    	mHandler.removeCallbacks(updateProgressBarTask);
    	
        if (isFinishing()) {
        	unbindService(mMusicServiceCon);
        	Parcel p = Parcel.obtain();
        	try {
        		//Bundle tempBackup = new Bundle();
        		//tempBackup = writeStateBundle(tempBackup);
        		//tempBackup.writeToParcel(p, 0);
        		writeStateParcel(p, 0);
        		FileOutputStream outputStream = openFileOutput(BACKUP_STATE, Context.MODE_PRIVATE);
        		outputStream.write(p.marshall());
        		outputStream.close();
        		} catch (Exception e) {
        			Log.e(LOG_TAG, "Couldn't save temporary backup file:"+e.toString());
        			e.printStackTrace();
        		} finally {
        			p.recycle();
        		}
        	
        	//stopService(new Intent(this, MusicService.class));
        }
    }
    
    
    @Override
    protected void onStop() {
        super.onStop(); 
        if (isFinishing()) mMusicService.unregisterObserver(null);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	/*IPlaylist<Track> playlist = getMusicData().getCurrentPlaylist();
    	if (playlist!=null) {
	  		  playlist.setTitle("RecentLast");
	  		  PlaylistDbHelper dbHelper = getMusicData().getPlaylistDbHelper();
			  dbHelper.savePlaylist(playlist);
	  		}*/
	  }

	
}