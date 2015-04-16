package com.notime2wait.simpleplayer.fragments;


import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.notime2wait.simpleplayer.MainActivity;
import com.notime2wait.simpleplayer.MusicData;
import com.notime2wait.simpleplayer.R;
import com.notime2wait.simpleplayer.StickyListAdapter;
import com.notime2wait.simpleplayer.MusicData.Track;
import com.notime2wait.simpleplayer.R.id;
import com.notime2wait.simpleplayer.R.layout;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.R.color;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class AllMusicListFrag extends Fragment{
	
	private static String LOG_TAG = AllMusicListFrag.class.getName();
	
	private int HEADER_LISTNUM_OFFSET = 0;
	private String[] folders;
	private Track[] tracks;
	private View headerView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		String[] temp_tracks = MainActivity.getMusicData().getTracks();
		tracks = new String[temp_tracks.length];
		System.arraycopy(temp_folders, 0, folders, 0, temp_folders.length);
		if (MainActivity.DEBUG) for (String str:folders) Log.d(LOG_TAG, str);*/
		tracks =  MainActivity.getMusicData().getTracks();
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
            Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            //This layout contains your list view 
                View view = inflater.inflate(R.layout.stickylist_frag, container, false);
                StickyListHeadersListView stickyList = (StickyListHeadersListView) view.findViewById(R.id.list);
                stickyList.setOnItemClickListener(new OnItemClickListener()
                {
                    

					@Override
					public void onItemClick(AdapterView<?> adapter, View v,
							int position, long id) {
						MainActivity.getMusicData().playTrack(position);
						MainActivity.slidingMenu.showContent(true);
					}
                });
                StickyListAdapter adapter = new StickyListAdapter(getActivity());
                if (MainActivity.DEBUG) Log.v(LOG_TAG, "Adapter"+adapter);
                if (MainActivity.DEBUG) Log.v(LOG_TAG, "List"+stickyList);
                stickyList.setAdapter(adapter);
                
              return view;
    }
	/*
	public class StickyListAdapter extends BaseAdapter implements StickyListHeadersAdapter {

	    //private String[] countries;
	    private LayoutInflater inflater;

	    @Override
	    public int getCount() {
	        return tracks.length;
	    }

	    @Override
	    public Object getItem(int position) {
	        return tracks[position];
	    }

	    @Override
	    public long getItemId(int position) {
	        return position;
	    }
	    
	   
	    @Override 
	    public View getHeaderView(int position, View convertView, ViewGroup parent) {
	        HeaderViewHolder holder;

	        if (convertView == null) {
	        	inflater = getActivity().getLayoutInflater();
	            holder = new HeaderViewHolder();
	            convertView = inflater.inflate(R.layout.folderlist_item, parent, false);
	            holder.folderName = (TextView)convertView.findViewById(R.id.folderName);
	      		holder.folderPath = (TextView)convertView.findViewById(R.id.folderPath);
	            convertView.setTag(holder);
	        } else {
	            holder = (HeaderViewHolder) convertView.getTag();
	        }

	        String folder_item = MainActivity.getMusicData().getFolderName(position);//folders[position];
		    //String folder_item = getItem(position);
		    
		    int slash_position = folder_item.lastIndexOf('/');
		    String name = folder_item.substring(slash_position);    
	        holder.folderName.setText(name);
	  		holder.folderPath.setText(folder_item);

	        return convertView;
	    }
	    

	    class HeaderViewHolder {
	    	TextView folderName;
	  		TextView folderPath;
	    }
	    

	    @Override 
	    public View getView(int position, View convertView, ViewGroup parent) {
	        ViewHolder holder;
	        if (convertView == null) {
	        	inflater = getActivity().getLayoutInflater();
	            holder = new ViewHolder();
	            convertView = inflater.inflate(R.layout.tracklist_item, parent, false);
	            holder.trackName = (TextView)convertView.findViewById(R.id.trackName);
	            convertView.setTag(holder);
	        } else {
	            holder = (ViewHolder) convertView.getTag();
	        }
	        holder.trackName.setText(tracks[position]);
	        return convertView;
	    }

	    @Override
	    public long getHeaderId(int position) {
	        //return the first character of the country as ID because this is what headers are based upon
	    	 String folder_item = MainActivity.getMusicData().getFolderName(position);//folders[position];
			    //String folder_item = getItem(position);
			    
			    int slash_position = folder_item.lastIndexOf('/');
			    String name = folder_item.substring(slash_position);
	        return name.subSequence(0, 1).charAt(0);
	    }

	    class ViewHolder {
	        TextView trackName;
	    }


	}
	
/*	private static String LOG_TAG = TrackListFrag.class.getName(); 
	
	private int HEADER_LISTNUM_OFFSET = 2;
	private MediaPlayer mMediaPlayer;
	private Map<String, ArrayList<String>> folderlist = new TreeMap<String, ArrayList<String>>();
	private ArrayAdapter<String> folderlistAdapter;
	String[] folders;
	private ArrayAdapter<String> tracklistAdapter;
	private boolean isFolderView = true;
	private View headerView;
	

		@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String[] temp_folders = MainActivity.getMusicData().getFolders();
		folders = new String[temp_folders.length];
		System.arraycopy(temp_folders, 0, folders, 0, temp_folders.length);
		if (MainActivity.DEBUG) for (String str:folders) Log.d(LOG_TAG, str);
	}
	
	  @Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    //Set<String> set = folderlist.keySet();
	    headerView = new TextView(getActivity());
	    ((TextView)headerView).setText("HEADER");
	    this.getListView().addHeaderView(headerView);
	    this.getListView().addHeaderView(headerView);
	    StickyListHeadersListView stickyList = (StickyListHeadersListView) getView().findViewById(R.id.stickyList);
	    MyAdapter adapter = new MyAdapter(this);
	    stickyList.setAdapter(adapter);
	    setListAdapter(sticky_list_adapter);
	}
	  
	  						
	  
	  @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		  //v.setBackgroundColor(color.background_light);
		  if (isFolderView) {
			  	  if (MainActivity.DEBUG) Log.d(LOG_TAG, "Position "+folders[position-HEADER_LISTNUM_OFFSET]);
				  tracklistAdapter = getTrackListAdapter(folders[position-HEADER_LISTNUM_OFFSET]);
				  setListAdapter(tracklistAdapter);
				  isFolderView = false;
		  }
		  else playTrack();
		  
	}
	  
	private ArrayAdapter<String> getFolderListAdapter() {
		  return new ArrayAdapter<String>(getActivity(),
			        R.layout.folderlist_item, folders) {
		    	

			  @Override
			  public View getView(int position, View convertView, ViewGroup parent) {
			    View folder_view = convertView;
			    if (folder_view == null) {
			       LayoutInflater inflater = getActivity().getLayoutInflater();
			    	//LayoutInflater inflater = LayoutInflater.from(getContext());
			    	folder_view = inflater.inflate(R.layout.folderlist_item, parent, false);
			    }

			    String folder_item = folders[position];
			    //String folder_item = getItem(position);
			    
			    int slash_position = folder_item.lastIndexOf('/');
			    String name = folder_item.substring(slash_position);
			    		
			    TextView folderName = (TextView)folder_view.findViewById(R.id.folderName);
	    		TextView folderPath = (TextView)folder_view.findViewById(R.id.folderPath);
	    		folderName.setText(name);
	    		folderPath.setText(folder_item);

			    return folder_view;
			  }
			  
		    }; 
	}
	  
	  private ArrayAdapter<String> getTrackListAdapter(String folder_path) {

		  if (MainActivity.DEBUG)
				Log.d(LOG_TAG, folder_path);
		  
		  final String[] tracks = MainActivity.getMusicData().getTracks(folder_path);
		  return new ArrayAdapter<String>(getActivity(),
			        R.layout.folderlist_item, tracks) {
		    	

			  @Override
			  public View getView(int position, View convertView, ViewGroup parent) {
			    View track_view = convertView;
			    if (track_view == null) {
			       LayoutInflater inflater = getActivity().getLayoutInflater();
			    	//LayoutInflater inflater = LayoutInflater.from(getContext());
			       track_view = inflater.inflate(R.layout.tracklist_item, parent, false);
			    }
			    		
			    TextView trackName = (TextView)track_view.findViewById(R.id.trackName);
	    		trackName.setText(tracks[position]);

			    return track_view;
			  }
			  
		    }; 
	  }
	  
	  	private void playTrack() {
	  		
	  	}
		
		

		  /*@Override
		  public void onDestroyView() {
			  setListAdapter(null);
		  }*/
}



