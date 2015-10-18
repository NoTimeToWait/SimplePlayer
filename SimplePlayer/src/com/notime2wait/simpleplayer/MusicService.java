package com.notime2wait.simpleplayer;

import com.notime2wait.simpleplayer.visualization.IVisuals;
import com.notime2wait.simpleplayer.visualization.WaveformUtils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.*;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashSet;
import java.util.Observer;
import java.util.Set;

public class MusicService extends Service {
	
	
	private final IBinder mBinder = new ServiceBinder();
	private boolean justStarted = true;
	private static final MusicData mMusicData = new MusicData();
	
	public static String INTENT_CATEGORY = "com.notime2wait.simpleplayer.player_intent";
	public static String ACTION_NEXT = "com.notime2wait.simpleplayer.next_track";
	public static String ACTION_PREV = "com.notime2wait.simpleplayer.previous_track";
	public static String ACTION_PLAY = "com.notime2wait.simpleplayer.play_track";
	public static String ACTION_PAUSE = "com.notime2wait.simpleplayer.pause_track";

	//TODO: have to check if there are any possible errors that will require creating a new instance of the MediaPlayer. Thus, removing "final"
		// property will require checking SessionId and properly updating it wherever it is used.
    private static final MediaPlayer mPlayer = new MediaPlayer();
    private RemoteViews mRemoteViews;
    private NotificationCompat.Builder mNotificationBuilder;
    private BroadcastReceiver mBroadcastReceiver;
    
    private final Set<TrackChangeObserver> observers = new HashSet<TrackChangeObserver>();
    
    public boolean justStarted() {
    	return justStarted;
    }
    
    public void setStarted(){
    	justStarted = false;
    }

    public static MediaPlayer getMediaPlayer() {
    	return mPlayer;
    }
    
	public static MusicData getMusicData() {
		return mMusicData;
	}
			
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	public void registerObserver(TrackChangeObserver obs) {
		observers.add(obs);
	}
	
	public void unregisterObserver(TrackChangeObserver obs) {
		observers.remove(obs);
	}
	
	private void notifyObservers(Track track, String albumImagePath) {
		for (TrackChangeObserver obs: observers)
			obs.update(track, albumImagePath, getDuration());
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
		//notifyObserversStop();
	}
	
	public void start() {
		mPlayer.start();
		if (mRemoteViews!=null) {
			mRemoteViews.setImageViewResource(R.id.btn_ntf_play, R.drawable.btn_pause_icn);
			updateNotification();
		}
	}
	
	public void pause() {
		mPlayer.pause();
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
			if (!prepareOnly) mPlayer.start();
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
		notifyObservers(track, track.getAlbumArt(true));
		return true;
	}
	
	public boolean  playTrack(Track track){
		prepareTrack(track, false);
		return true;
    }
	
	
	
	private Track getCurrentTrack() {
    	return mMusicData.getCurrentPlaylist().getCurrentTrack();
    }
	
    private Track getNextTrack() {
    	return mMusicData.getCurrentPlaylist().getNext();
    }
    
    private Track getPrevTrack() {
    	return mMusicData.getCurrentPlaylist().getPrev();
    }
    /*
    private Track getRandomTrack() {
    	return mMusicData.new Track("", "", "", "");
    }
	*/
	@Override
    public void onCreate (){
	  super.onCreate();
	  if (mBroadcastReceiver==null) {
		  IntentFilter filter = new IntentFilter();
		  filter.addAction(ACTION_NEXT);
		  filter.addAction(ACTION_PREV);
		  filter.addAction(ACTION_PLAY);
		  //filter.addAction(ACTION_PAUSE);
		  mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.e("CONTEXT"+context, "INTENT"+intent.getAction());

				String action = intent.getAction();
				if (mMusicData.getCurrentPlaylist().getPlaylistSize()==0) return;
				if (ACTION_NEXT.equals(action)) playNext();
				if (ACTION_PREV.equals(action)) playPrevious();
				if (ACTION_PLAY.equals(action)) {
					if (isPlaying()) pause(); 
					else start();		
				}
			}
		  };
		  //this.registerReceiver(mBroadcastReceiver, filter);
		  LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);
	  }
	   mMusicData.init(this);
       

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
		
		Track track = mMusicData.getCurrentTrack();
		mRemoteViews.setTextViewText(R.id.notification_descr, track.getTitle()/*+" - "+track.getArtist()*/);
		//mRemoteViews.setImageViewBitmap(R.id.notification_icon, mMusicData.get)
		startForeground(1, mNotificationBuilder.build());
		isForeground = true;
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
	
	public interface TrackChangeObserver {
		
		public void update(Track track, String albumImagePath, int duration);
	}
	
	@Override
    public void onDestroy() {
    	super.onDestroy();
    	if (mBroadcastReceiver!=null) LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
	}
	
	

}
