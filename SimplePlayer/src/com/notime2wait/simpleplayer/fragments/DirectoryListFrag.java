package com.notime2wait.simpleplayer.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.notime2wait.simpleplayer.MainActivity;
import com.notime2wait.simpleplayer.MusicData;
import com.notime2wait.simpleplayer.R;
import com.notime2wait.simpleplayer.SwipeDismissListViewTouchListener;
import com.notime2wait.simpleplayer.Track;

import java.util.Arrays;
import java.util.Comparator;


public class DirectoryListFrag extends BackHandledListFragment{
	
	private static String LOG_TAG = DirectoryListFrag.class.getName(); 
	
	private int HEADER_LISTNUM_OFFSET = 0;
	private ArrayAdapter<String> folderlistAdapter;
	private ArrayAdapter<Track> tracklistAdapter;
	private MusicData mMusicData;
	private String[] folders;
	private Track[] currentFolderTracks;
	private String currentFolder;
	private boolean isFolderView = true;
	private View headerView;
	

		@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMusicData = MainActivity.getMusicData();
		/*
		String[] temp_folders = mMusicData.getFolders();
		folders = new String[temp_folders.length];
		System.arraycopy(temp_folders, 0, folders, 0, temp_folders.length);*/
		folders = mMusicData.getFolders().toArray(new String[0]);
		Arrays.sort(folders, new Comparator<String>(){
			public int compare(String str1, String str2) {
				int slash_position = str1.lastIndexOf('/');
			    String new_str1 = str1.substring(slash_position);
			    slash_position = str2.lastIndexOf('/');
			    String new_str2 = str2.substring(slash_position);
		        return new_str1.compareTo(new_str2);
		    }
		});
		if (MainActivity.DEBUG) for (String str:folders) Log.d(LOG_TAG, str);
		
		
	}
			
	  @Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    //Set<String> set = folderlist.keySet();
	    
	    headerView = new TextView(getActivity());
	    ((TextView)headerView).setText("HEADER");
	   // this.getListView().addHeaderView(headerView);
	   // this.getListView().addHeaderView(headerView);
	    folderlistAdapter = getFolderListAdapter();
	    setListAdapter(folderlistAdapter);
	    setListShownNoAnimation(true);
	    ListView listView = this.getListView();
	    listView.setDivider(null);
		SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        listView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                            	if (isFolderView)
                            		for (int position : reverseSortedPositions) {
                            			String folder_path = folders[position-HEADER_LISTNUM_OFFSET];
                            			mMusicData.addTracksToPlaylist(folder_path, mMusicData.getTracks(folder_path).length);
                            		}
                            	else 
                            		for (int position : reverseSortedPositions) {
                                        mMusicData.addTrackToPlaylist(currentFolder, position-HEADER_LISTNUM_OFFSET);
                                		}
                                //mAdapter.notifyDataSetChanged();
                            }

							@Override
							public boolean canDismiss(int position) {
								//TODO: add here @fling_dismiss option from User Preferences
								return true;
							}
                        });
        listView.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        listView.setOnScrollListener(touchListener.makeScrollListener());
	    
	}
	  
	  						
	  
	  @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		  //v.setBackgroundColor(color.background_light);
		  if (isFolderView) {
			  saveScrollState();
			  currentFolder = folders[position-HEADER_LISTNUM_OFFSET];
			  if (MainActivity.DEBUG) Log.d(LOG_TAG, "Position "+folders[position-HEADER_LISTNUM_OFFSET]);
			  tracklistAdapter = getTrackListAdapter(currentFolder);
			  setListAdapter(tracklistAdapter);
			  isFolderView = false;
		  }
		  else {
			  mMusicData.playTrack(currentFolder, position, currentFolderTracks.length);
			  MainActivity.slidingMenu.showContent(true);
		  }
		  
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
			    		
			    TextView folderName = (TextView)folder_view.findViewById(R.id.title);
	    		TextView folderPath = (TextView)folder_view.findViewById(R.id.folderPath);
	    		folderName.setText(name);
	    		folderPath.setText(folder_item.replaceFirst("(/storage/)?(.*)(/.*)", "$2"));

			    return folder_view;
			  }
			  
		    }; 
	}
	  
	  private ArrayAdapter<Track> getTrackListAdapter(String folder_path) {

		  if (MainActivity.DEBUG)
				Log.d(LOG_TAG, folder_path);
		  
		  currentFolderTracks = mMusicData.getTracks(folder_path);
		  return new ArrayAdapter<Track>(getActivity(),
			        R.layout.tracklist_item, currentFolderTracks) {
		    	

			  @Override
			  public View getView(int position, View convertView, ViewGroup parent) {
			    View track_view = convertView;
			    if (track_view == null) {
			       LayoutInflater inflater = getActivity().getLayoutInflater();
			    	//LayoutInflater inflater = LayoutInflater.from(getContext());
			       track_view = inflater.inflate(R.layout.tracklist_item, parent, false);
			    }
			    		
			    TextView trackName = (TextView)track_view.findViewById(R.id.title);
	    		trackName.setText(currentFolderTracks[position].getTitle());

			    return track_view;
			  }
			  
		    }; 
	  }
	  /*
	  private void getMusicList() {
			String[] music_list;
			Cursor cursor;
	        Uri all_music_uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
	        String select = "("+MediaStore.Audio.Media.IS_MUSIC + " != 0) AND (" + MediaStore.Audio.Media.DATA +" != '')";
			String[] proj = { MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE };
			System.out.println(all_music_uri);
			if (isSdMounted()) {
				CursorLoader loader = new CursorLoader(this.getActivity(), all_music_uri, proj, select, null, null);
				cursor = loader.loadInBackground();
				if (cursor.moveToFirst()) {
					int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
					if (MainActivity.DEBUG) System.out.println(cursor.getCount());
					int i=0;
					String music_data, music_folder="", trackname;
					ArrayList<String> tracklist = new ArrayList<String>();
					
					do {
						
						music_data = cursor.getString(column_index);
						int slash_position = music_data.lastIndexOf('/');
						if (slash_position<2) continue;
						trackname = music_data.substring(slash_position+1)+"+"+cursor.getString(1);
						music_data = music_data.substring(0, slash_position);
						
						if (!music_folder.equals(music_data)) {
							if (i>0) folderlist.put(music_folder, tracklist);
							music_folder = music_data;
							//System.out.println(trackname);
							tracklist = new ArrayList<String>();
						}
						tracklist.add(trackname);
						
						i++;
						} while (cursor.moveToNext());
				}
					
			}
		}
	  
	  
		private static boolean isSdMounted() 
		{
		    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		}
	  */
		
		

	@Override
	public String getTagText() {
		return "DirectoryTab";
	}

	@Override
	public boolean onBackPressed() {
		if (!isFolderView && MainActivity.slidingMenu.isMenuShowing()) {
			/* returns back to menu from the front slide - deprecated
			 * if (!MainActivity.slidingMenu.isMenuShowing()) {
				MainActivity.slidingMenu.showMenu();
				return true;
			} */
			setListAdapter(folderlistAdapter);
			isFolderView = true;
			restoreScrollState();
			return true; //back event is consumed
		}
		return false;	//back event is not consumed
	}
	


  	@Override
	public void onDestroyView() {
  		super.onDestroyView();
		setListAdapter(null);
		isFolderView = true;
		MainActivity.handleUndoAction(null); //hide any popups produced by this fragment
	}
}

