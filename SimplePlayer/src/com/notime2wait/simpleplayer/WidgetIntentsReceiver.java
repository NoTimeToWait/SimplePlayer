package com.notime2wait.simpleplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class WidgetIntentsReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("CONTEXT"+context, "INTENT"+intent.getAction());
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
	    if (manager == null) return;
	    
	    Intent wrappedIntent = new Intent(intent);
	    wrappedIntent.setComponent(null);
	    manager.sendBroadcast(wrappedIntent);
		/*if ((MusicService.ACTION_NEXT).equals(action)) playNext();
		if ((MusicService.ACTION_PREV).equals(action)) playPrevious();
		if ((MusicService.ACTION_PLAY).equals(action)) start();
		if ((MusicService.ACTION_PAUSE).equals(action)) pause();*/			
	}

}
