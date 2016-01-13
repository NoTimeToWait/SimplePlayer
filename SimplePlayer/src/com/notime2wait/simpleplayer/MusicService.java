package com.notime2wait.simpleplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MusicService extends Service {
	
	
	private final IBinder mBinder = new ServiceBinder();
	private boolean justStarted = true;
	//private static final MusicData mMusicData = new MusicData();
	
	public static String INTENT_CATEGORY = "com.notime2wait.simpleplayer.player_intent";
	public static String ACTION_NEXT = "com.notime2wait.simpleplayer.next_track";
	public static String ACTION_PREV = "com.notime2wait.simpleplayer.previous_track";
	public static String ACTION_PLAY = "com.notime2wait.simpleplayer.play_track";
	public static String ACTION_PAUSE = "com.notime2wait.simpleplayer.pause_track";
	
	public static int SOURCE_MUSIC_PLAY = 2;
	public static int SOURCE_TRACK_CHANGED = 4;

	//TODO: have to check if there are any possible errors that will require creating a new instance of the MediaPlayer. Thus, removing "final"
		// property will require checking SessionId and properly updating it wherever it is used.
    private static final MediaPlayer mPlayer = new MediaPlayer();
    private RemoteViews mRemoteViews;
    private NotificationCompat.Builder mNotificationBuilder;
    private BroadcastReceiver mBroadcastReceiver;
    private BroadcastReceiver mHeadsetStateReceiver;
    //private RemoteControlReceiver mRemoteControlReceiver;
    private boolean mAudioFocusGranted = false;
    private boolean wasPlayingOnFocusChange = false;

	public boolean isShuffle;
	public boolean isRepeat;
	//private ListIterator<Track> shuffledPlaylistIterator;
	private ArrayList<Track> shuffledPlaylist;
	private int shuffledIndex;
    
    private final Set<MusicServiceObserver> observers = new HashSet<MusicServiceObserver>();
    
    private OnAudioFocusChangeListener audioFocusListener = new OnAudioFocusChangeListener() {
	    public void onAudioFocusChange(int focusChange) {
	    	
	    	switch (focusChange) {
	    		case AudioManager.AUDIOFOCUS_GAIN:
	    			mAudioFocusGranted = true;
	    			if(wasPlayingOnFocusChange) 
				    	start();
	    		break;
	    		case AudioManager.AUDIOFOCUS_LOSS:
	    			((AudioManager) MusicService.this.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(this);
	    		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	    		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	    			mAudioFocusGranted = false;
	    			wasPlayingOnFocusChange = mPlayer.isPlaying();
	    			pause();
	    			break;
	    	}
	    	
	    }
	};
    
    public boolean justStarted() {
    	return justStarted;
    }
    
    public void setStarted(){
    	justStarted = false;
    }

    public static MediaPlayer getMediaPlayer() {
    	return mPlayer;
    }

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	public void registerObserver(MusicServiceObserver obs) {
		observers.add(obs);
	}
	
	public void unregisterObserver(MusicServiceObserver obs) {
		if (obs==null) observers.clear();
		else observers.remove(obs);
	}
	
	private void notifyObservers(Bundle extras, int source) {
		if (isPlaying()) source|=SOURCE_MUSIC_PLAY;
		for (MusicServiceObserver obs: observers)
			obs.update(extras, source);
	}
		
	public boolean requestAudioFocus() {
		if (!mAudioFocusGranted) {
			AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
			int result = am.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				//am.registerMediaButtonEventReceiver(RemoteControlReceiver);
				mAudioFocusGranted = true;
			}
		}
		return mAudioFocusGranted;
	}
	
	
	
	public int getDuration() {
		return mPlayer.getDuration();
	}
	
	public int getSessionId() {
		return mPlayer.getAudioSessionId();
	}
	
	public int getCurrentPosition() {
		return mPlayer.getCurrentPosition();
	}
	
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}
	
	public void stopMusic() {
		mPlayer.stop();
		if (mRemoteViews!=null) {
			mRemoteViews.setImageViewResource(R.id.btn_ntf_play, R.drawable.btn_play_icn);
			updateNotification();
		}
		//notifyObserversStop();
	}
	
	public void start() {
		mPlayer.setVolume(0.8f, 0.9f);
		mPlayer.start();
		notifyObservers(null, 0);
		if (mRemoteViews!=null ) {
			mRemoteViews.setImageViewResource(R.id.btn_ntf_play, R.drawable.btn_pause_icn);
			updateNotification();
		}
	}
	
	public void pause() {
		mPlayer.pause();
		notifyObservers(null, 0);
		if (mRemoteViews!=null) {
			mRemoteViews.setImageViewResource(R.id.btn_ntf_play, R.drawable.btn_play_icn);
			updateNotification();
		}
	}
	
	public void seekTo(int msec) {
		mPlayer.seekTo(msec);
	}
	
	public void playNext() {
		Track trackToPlay = getNextTrack();
        //if(isRepeat)  trackToPlay = getCurrentTrack();
       // else if(isShuffle) trackToPlay = getRandomTrack();
       // else trackToPlay = getNextTrack();
        if (trackToPlay!=null) playTrack(trackToPlay);
	}
	
	public void playPrevious() {
		Track trackToPlay = getPrevTrack();
        if (trackToPlay!=null) playTrack(trackToPlay);
	}
	
	public boolean prepareTrack(Track track, boolean prepareOnly) {
		try {
			mPlayer.reset();
			mPlayer.setDataSource(track.getPath());
			mPlayer.prepare();
			if (!prepareOnly && requestAudioFocus()) mPlayer.start();
			if (mRemoteViews!=null) {
					mRemoteViews.setTextViewText(R.id.notification_descr, track.getTitle()/*+" - "+track.getArtist()*/);
					updateNotification();
			}
		} catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
			e.printStackTrace();
		}
		Bundle extras = new Bundle();
		int source = SOURCE_TRACK_CHANGED;
		extras.putParcelable("track", track);
		notifyObservers(extras, source);
		return true;
	}
	
	public boolean  playTrack(Track track){
		prepareTrack(track, false);
		return true;
    }

	public void shufflePlaylist() {
		shuffledPlaylist = new ArrayList<Track>();
		shuffledPlaylist.addAll(getCurrentPlaylist().getTracks());
		shuffledPlaylist.remove(getCurrentPlaylist().getCurrentTrack());
		Collections.shuffle(shuffledPlaylist, new Random(System.nanoTime()));
		shuffledPlaylist.add(0, getCurrentPlaylist().getCurrentTrack());
		//shuffledPlaylistIterator = shuffledPlaylist.listIterator();
		shuffledIndex = 0;
	}


    private IPlaylist<Track> getCurrentPlaylist() {
        return MusicData.getInstance().getCurrentPlaylist();
    }
	
    private Track getNextTrack() {
		Track next = null;
		if (isShuffle) {
			if (shuffledIndex >= shuffledPlaylist.size()-1 && isRepeat) {
				//shuffledPlaylistIterator = shuffledPlaylist.listIterator();
				//next = shuffledPlaylistIterator.next();
				shuffledIndex = -1;
				//next = shuffledPlaylist.get(shuffledIndex);
			}

			if (shuffledIndex < shuffledPlaylist.size()-1) {
				next = shuffledPlaylist.get(++shuffledIndex);
				int index = getCurrentPlaylist().getTracks().indexOf(next);
				getCurrentPlaylist().setCurrentTrackIndex(index);
			}
		}
    	else next = getCurrentPlaylist().getNext();
		if (next==null && isRepeat) next = getCurrentPlaylist().getFirst();
		return next;
    }

    
    private Track getPrevTrack() {
		Track prev = null;
		if (isShuffle) {
			if (shuffledIndex==0&&isRepeat) {
				//shuffledPlaylistIterator = shuffledPlaylist.listIterator(shuffledPlaylist.size());
				//prev = shuffledPlaylistIterator.previous();
				shuffledIndex = shuffledPlaylist.size();
				//prev = shuffledPlaylist.get(shuffledIndex);
			}
			if (shuffledIndex>0) {
				prev = shuffledPlaylist.get(--shuffledIndex);
				int index = getCurrentPlaylist().getTracks().indexOf(prev);
				getCurrentPlaylist().setCurrentTrackIndex(index);
			}
		}
		else prev = getCurrentPlaylist().getPrev();
		if (prev==null && isRepeat) prev = getCurrentPlaylist().getLast();
		return prev;
    }
    /*
    private Track getRandomTrack() {
    	return mMusicData.new Track("", "", "", "");
    }
	*/
	@Override
    public void onCreate (){
	    super.onCreate();
        MusicData.init(this);
	    if (mBroadcastReceiver==null) {
		    IntentFilter filter = new IntentFilter();
		    filter.addAction(ACTION_NEXT);
		    filter.addAction(ACTION_PREV);
		    filter.addAction(ACTION_PLAY);
		    //filter.addAction(ACTION_PAUSE);
		    mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d("CONTEXT"+context, "INTENT"+intent.getAction());

				String action = intent.getAction();
				if (getCurrentPlaylist().getPlaylistSize()==0) return;
				if (ACTION_NEXT.equals(action)) playNext();
				if (ACTION_PREV.equals(action)) playPrevious();
				if (ACTION_PLAY.equals(action)) {
					if (!requestAudioFocus()) return;
					if (isPlaying()) pause(); 
					else start();		
				}
			}
		  };
		  //this.registerReceiver(mBroadcastReceiver, filter);

		    LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);
	    }
	  
	    if (mHeadsetStateReceiver==null) {
		    IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		    filter.addAction(ACTION_NEXT);
		    mHeadsetStateReceiver = new BroadcastReceiver() {
		  @Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getIntExtra("state", 1)==0) pause();
				
			}
		  };
		    registerReceiver(mHeadsetStateReceiver, filter);
	    }

       

	    mPlayer.setOnCompletionListener( new OnCompletionListener() {
		   @Override
		    public void onCompletion(MediaPlayer mp) {
				playNext();
		        // check for repeat is ON or OFF
				/**/
		    }
	    });
        mPlayer.setOnErrorListener(new OnErrorListener() {

        	@Override
        	public boolean onError(MediaPlayer mp, int what, int extra) {

        		
        		if(mp != null)
        			try{
        				mp.stop();
        				mp.reset();
        			} finally {
        				//TODO: implement
        			}
        		else Toast.makeText(MusicService.this, "music player failed", Toast.LENGTH_SHORT).show();
        		return false;
        	}
        });
	}

	public class ServiceBinder extends Binder {
		MusicService getService()
   	 	{
			return MusicService.this;
   	 	}
		
	}
	
	private boolean isForeground = false;
	
	public void startForegroundService() {
		mRemoteViews = new RemoteViews(getPackageName(),  
                R.layout.player_widget);  
		mNotificationBuilder = new NotificationCompat.Builder(  
                this).setSmallIcon(R.drawable.ic_launcher).setContent(  
                mRemoteViews);  
		// Creates an explicit intent for an Activity in your app  
		Intent openPlayerIntent = new Intent(this, MainActivity.class);   
		//TODO: do not remove, have to ensure we don't need TaskStack here
		/*TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);  
		stackBuilder.addParentStack(MainActivity.class);  
		stackBuilder.addNextIntent(resultIntent);  
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,  
                PendingIntent.FLAG_UPDATE_CURRENT);  */
		PendingIntent openPlayerPIntent = PendingIntent.getActivity(this, 0, openPlayerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mRemoteViews.setOnClickPendingIntent(R.id.notification_icon, openPlayerPIntent); 
		
		Intent pauseIntent =new Intent(this, WidgetIntentsReceiver.class).setAction(ACTION_PLAY);
		PendingIntent pausePIntent = PendingIntent.getBroadcast(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mRemoteViews.setOnClickPendingIntent(R.id.btn_ntf_play, pausePIntent);
		
		//Intent prevIntent = new Intent(ACTION_PREV).setClass(this, WidgetIntentsReceiver.class);
		Intent prevIntent = new Intent(this, WidgetIntentsReceiver.class).setAction(ACTION_PREV);
		PendingIntent prevPIntent = PendingIntent.getBroadcast(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mRemoteViews.setOnClickPendingIntent(R.id.btn_ntf_prev, prevPIntent);
		
		//Intent nextIntent = new Intent(ACTION_NEXT).setClass(this, WidgetIntentsReceiver.class);
		Intent nextIntent = new Intent(this, WidgetIntentsReceiver.class).setAction(ACTION_NEXT);
		PendingIntent nextPIntent = PendingIntent.getBroadcast(this, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mRemoteViews.setOnClickPendingIntent(R.id.btn_ntf_next, nextPIntent);
		
		Track track = getCurrentPlaylist().getCurrentTrack();
		mRemoteViews.setTextViewText(R.id.notification_descr, track.getTitle()/*+" - "+track.getArtist()*/);
		//mRemoteViews.setImageViewBitmap(R.id.notification_icon, mMusicData.get)
		startForeground(1, mNotificationBuilder.build());
		isForeground = true;
	}
	
	private void updateWidgets() {
		//TODO:
	}
	
	private void updateNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(1, mNotificationBuilder.build());
	}
	
	public void stopForegroundService() {
		stopForeground(true);
		mRemoteViews = null;
		isForeground = false;
	}
	
	public boolean isForeground() {
		return isForeground;
	}
	
	public interface MusicServiceObserver {
		public void update(Bundle extras, int source);
	}
	
	
	@Override
    public void onDestroy() {
    	super.onDestroy();
    	if (mHeadsetStateReceiver!=null) {
    		unregisterReceiver(mHeadsetStateReceiver);
    		mHeadsetStateReceiver = null;
    	}
    	if (mBroadcastReceiver!=null) {
    		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    		mBroadcastReceiver = null;
    	}
		if (mAudioFocusGranted) ((AudioManager) this.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(audioFocusListener);
	}
	
	

}
