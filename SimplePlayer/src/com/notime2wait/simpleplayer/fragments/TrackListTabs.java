package com.notime2wait.simpleplayer.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.notime2wait.simpleplayer.MainActivity;
import com.notime2wait.simpleplayer.R;

public class TrackListTabs extends Fragment{
	
	private static String LOG_TAG = TrackListTabs.class.getName(); 
	
	private FragmentTabHost mTabHost;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {


        //System.out.println(getChildFragmentManager());
        //System.out.println("Activity:"+getActivity());
        mTabHost = new FragmentTabHost(getActivity());//mTabHost = (FragmentTabHost) getView().findViewById(android.R.id.tabhost);
       //TODO should i set 
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.realtabcontent);
        mTabHost.addTab(mTabHost.newTabSpec("tracklist").setIndicator("Now Playing"),
                TrackListFrag.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("directory").setIndicator("Folders"),
                DirectoryListFrag.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("playlist").setIndicator("Playlists"),
                PlaylistsFrag.class, null);
       // mTabHost.addTab(mTabHost.newTabSpec("AllMusicTab").setIndicator("All Music"),
       //         AllMusicListFrag.class, null);
        mTabHost.setCurrentTabByTag("directory");
        //int childCount = mTabHost.getTabWidget().getChildCount();
        //String text = ((TextView) mTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title)).getText().toString();
        //float textSize = ((TextView) mTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title)).getTextSize();
        for(int i=0;i<mTabHost.getTabWidget().getChildCount();i++)
        {
            TextView tv = (TextView) mTabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        }
        MainActivity.setTracklistTabHost(mTabHost);
        return mTabHost;
    }
	
	public void onDestroyView() {
        super.onDestroyView();
        mTabHost = null;
    }
	/*
	public void onBackPressed() {
	    boolean isPopFragment = false;
	    String currentTabTag = mTabHost.getCurrentTabTag();
	    if (currentTabTag.equals(TAB_1_TAG)) {
	        isPopFragment = ((BaseContainerFragment)getSupportFragmentManager().findFragmentByTag(TAB_1_TAG)).popFragment();
	    } else if (currentTabTag.equals(TAB_2_TAG)) {
	        isPopFragment = ((BaseContainerFragment)getSupportFragmentManager().findFragmentByTag(TAB_2_TAG)).popFragment();
	    }
	    if (!isPopFragment) {
	        finish();
	    }
	}*/

}
