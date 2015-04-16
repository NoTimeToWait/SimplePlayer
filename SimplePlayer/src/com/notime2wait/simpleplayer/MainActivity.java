package com.notime2wait.simpleplayer;

import java.io.IOException;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.notime2wait.simpleplayer.MusicData.Track;
import com.notime2wait.simpleplayer.UndoBarController.Undoable;
import com.notime2wait.simpleplayer.visualization.IVisuals;
import com.notime2wait.simpleplayer.visualization.WaveformUtils;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity 
		implements OnCompletionListener, SeekBar.OnSeekBarChangeListener{
	//implements AdapterView.OnItemClickListener, StickyListHeadersListView.OnHeaderClickListener {

    //private TestBaseAdapter mAdapter;
	private static String LOG_TAG = MainActivity.class.getName(); 
	private static int PROGRESS_UPDATE_TIME = 1000;
	private static MusicData mMusicData = new MusicData();
	private static IVisuals mVisuals;
	
	public static float density; 
	public static boolean DEBUG = true;
	public static SlidingMenu slidingMenu;
    private final MediaPlayer mPlayer = new MediaPlayer();
    private static int mSessionId;
    private static UndoBarController mUndoBarController;
    
	private ImageButton mBtnPlay;
    private ImageButton mBtnNext;
    private ImageButton mBtnPrev;
    private ImageButton mBtnRepeat;
    private ImageButton mBtnShuffle;
    private SeekBar mProgressBar;
    private TextView mTitle;
    private TextView mTitleDescr;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    private BgView mBackground;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();
    
    //private int currentTrackIndex = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
	
	public static MusicData getMusicData() {
		return mMusicData;
	}
	
	public static int getSessionId() {
		return mSessionId;
	}
	
	public static IVisuals getVisuals() {
		return mVisuals;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        getWindow().setBackgroundDrawable(null);
		//getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
	    //getActionBar().hide();
        
        setContentView(R.layout.activity_main);
	    setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mMusicData.init(this);
        mSessionId = mPlayer.getAudioSessionId();
        mUndoBarController =  new UndoBarController(findViewById(R.id.undobar));
        
        slidingMenu = (SlidingMenu) findViewById(R.id.slidingmenu);
        slidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
        slidingMenu.setSecondaryMenu(R.layout.right_slide);
        slidingMenu.showMenu(false);
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
        mBackground = (BgView) findViewById(R.id.background);
        mBackground.setDefaultBackground();
        
        mBtnPlay = getButtonPlay();
        mBtnPrev = getButtonPrev();
        mBtnNext = getButtonNext();
        //mBtnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
        //mBtnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
        mProgressBar = (SeekBar) findViewById(R.id.music_progressbar);
        mProgressBar.setProgressDrawable(null);;;;
        
        
        mVisuals = new WaveformUtils(getResources().getDisplayMetrics().widthPixels-(int)(16*density), (int) (45*density), this);

        mTitle = (TextView) findViewById(R.id.track_label);
        mTitleDescr = (TextView) findViewById(R.id.track_label_descr);
        songCurrentDurationLabel = (TextView) findViewById(R.id.current_duration);
        songTotalDurationLabel = (TextView) findViewById(R.id.total_duration);
        
        mProgressBar.setOnSeekBarChangeListener(this); // Important
        mPlayer.setOnCompletionListener(this);
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
    	return mUndoBarController.handleUndoToken(undoToken);
    }
    
    private String milliSecondsToTimer(long milliseconds){
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
 
    /**
     * Function to change progress to timer
     * @param progress -
     * @param totalDuration
     * returns current duration in milliseconds
     * */
    public int progressToTimer(int progress, int totalDuration) {
        int currentDuration = 0;
        totalDuration = (int) (totalDuration / 1000);
        currentDuration = (int) ((((double)progress) / 100) * totalDuration);
 
        // return current duration in milliseconds
        return currentDuration * 1000;
    }
            
    public boolean  playTrack(Track track){
        // Play song
        try {
            mPlayer.reset();
            mPlayer.setDataSource(track.getPath());
            if (MainActivity.DEBUG) Log.e(LOG_TAG, track.getPath());
            mPlayer.prepare();
            mPlayer.start();
            // Displaying Song title
            //String songTitle = songsList.get(songIndex).get("songTitle");
            /*
            //Uri musicSourceUri = mMusicData.getMusicSourceUri();
            String select = "("+MediaStore.Audio.Media.IS_MUSIC + " != 0) AND (" + MediaStore.Audio.Media.DATA +" != '')";
    		String[] proj = { MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE };
    		
    		CursorLoader loader = new CursorLoader(this, Uri.parse(songPath), proj, select, null, null);
    		Cursor cursor = loader.loadInBackground();
            Log.e(LOG_TAG, ""+cursor);
    		//TODO could generate errors, change
            mTitle.setText(cursor.getString(1));
 			*/
            mTitle.setText(track.getTitle());
            
            // Changing Button Image to pause image
            //btnPlay.setImageResource(R.drawable.btn_pause);
 
            // set Progress bar values from 0 to track_length_in_seconds
            mProgressBar.setProgress(0);
            mProgressBar.setMax(mPlayer.getDuration()/PROGRESS_UPDATE_TIME);
            
            //mProgressBar.setBackground(mVisuals.)
            // Updating progress bar
            
             
             mHandler.postDelayed(updateProgressBarTask, PROGRESS_UPDATE_TIME);
             mVisuals.setTrack(track);
             
             //TODO: Change method to setBgResource
             BitmapDrawable drawable = new BitmapDrawable(((WaveformUtils) mVisuals).getBitmap());
             //mProgressBar.setBackgroundDrawable(getResources().getDrawable(R.id.waveform));
             mProgressBar.setBackgroundDrawable(drawable);;
            return true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private Runnable updateProgressBarTask = new Runnable() {
        public void run() {
            songTotalDurationLabel.setText(milliSecondsToTimer(mPlayer.getDuration()));
            songCurrentDurationLabel.setText(milliSecondsToTimer(mPlayer.getCurrentPosition()));
            mProgressBar.setProgress(mPlayer.getCurrentPosition()/PROGRESS_UPDATE_TIME);
            mHandler.postDelayed(this, PROGRESS_UPDATE_TIME);
        }
     };
    
     public void stopMusic() {
    	 mPlayer.stop();
     }
     
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
        mPlayer.seekTo(seekBar.getProgress()*PROGRESS_UPDATE_TIME);
 
        // update timer progress again
        mHandler.postDelayed(updateProgressBarTask, PROGRESS_UPDATE_TIME);
    }
    

	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
    public void onCompletion(MediaPlayer mp) {
 
        // check for repeat is ON or OFF
		Track trackToPlay;
        if(isRepeat)  trackToPlay = getCurrentTrack();
        else if(isShuffle) trackToPlay = getRandomTrack();
        else trackToPlay = getNextTrack();
        playTrack(trackToPlay);
    }
    //TODO:finish these methods
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}
	
	private Track getCurrentTrack() {
    	return mMusicData.new Track("", "");
    }
	
    private Track getNextTrack() {
    	return mMusicData.new Track("", "");
    }
    
    private Track getPrevTrack() {
    	return mMusicData.new Track("", "");
    }
    
    private Track getRandomTrack() {
    	return mMusicData.new Track("", "");
    }
    
    private ImageButton getButtonNext(){

        ImageButton mBtnNext = (ImageButton) findViewById(R.id.btn_next);
        mBtnNext.setOnClickListener(new View.OnClickListener() {
    		 
            @Override
            public void onClick(View arg0) {
                playTrack(getNextTrack());
            }
        });
        return mBtnNext;
    }
    
    private ImageButton getButtonPrev(){

        ImageButton mBtn = (ImageButton) findViewById(R.id.btn_prev);
        mBtn.setOnClickListener(new View.OnClickListener() {
    		 
            @Override
            public void onClick(View arg0) {
                playTrack(getPrevTrack());
            }
        });
        return mBtn;
    }
    
    private ImageButton getButtonPlay(){

        ImageButton mBtn = (ImageButton) findViewById(R.id.btn_play);
        mBtn.setOnClickListener(new View.OnClickListener() {
 
            @Override
            public void onClick(View arg0) {
                // check for already playing
                if(mPlayer.isPlaying()){
                    	mPlayer.pause();

                    	if (DEBUG) Log.e("WWWWW", "PPPPP");
                        // Changing button image to play button
                        //mBtnPlay.setImageResource(R.drawable.btn_play);
                }else{
                    	mPlayer.start();
                    	if (DEBUG) Log.e("WWWWW", "SSSSS");
                        // Changing button image to pause button
                        //mBtnPlay.setImageResource(R.drawable.btn_pause);
                }
 
            }
        });
        return mBtn;
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
    protected void onPause() {
        super.onPause();

        if (isFinishing() && mPlayer != null) {
        	mHandler.removeCallbacks(updateProgressBarTask);
            mPlayer.release();
        }
    }
}